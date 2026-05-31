package com.jialli.first_ai_project.rag.service;

import com.jialli.first_ai_project.rag.config.data.PgVectorStoreConfigData;
import com.jialli.first_ai_project.rag.config.data.RagConfigData;
import com.jialli.first_ai_project.rag.config.data.RagConstants;
import com.jialli.first_ai_project.rag.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.print.Doc;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.qos.logback.core.util.Loader.getResources;

@Slf4j
@Component
public class RagIngestionService {
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final RagConfigData ragConfigData;
    private final PdfDocumentReaderConfig pdfDocumentReaderConfig;
    private final TokenTextSplitter tokenTextSplitter;
    private final String ragVectorStoreTableName;

    public RagIngestionService(@Qualifier("ragVectorStore") VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate,
                               RagConfigData ragConfigData,
                               TokenTextSplitter tokenTextSplitter,
                               PgVectorStoreConfigData pgVectorStoreConfigData) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.ragConfigData = ragConfigData;
        RagConfigData.PdfProperties pdfProperties = ragConfigData.getPdf();
        //Build a simple PDF reading config
        this.pdfDocumentReaderConfig = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(pdfProperties.getPagesPerDocument())
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withLeftAlignment(pdfProperties.isLeftAlignment())
                        .withNumberOfBottomTextLinesToDelete(pdfProperties.getNumberOfBottomTextLinesToDelete())
                        .withNumberOfTopPagesToSkipBeforeDelete(pdfProperties.getNumberOfTopTextLinesToDelete())
                        .build())
                .build();
        this.tokenTextSplitter = tokenTextSplitter;
        this.ragVectorStoreTableName = pgVectorStoreConfigData.getTableNameForRag();
    }


    public void initializePgVectorStore() throws RagException {
        if (skipIngest(jdbcTemplate)) {
            return;
        }
        var pdfResources = getResources();
        if (pdfResources == null) {
            return;
        }
        ingestDocumentChunksToVectoreStore(pdfResources);
    }




    private boolean skipIngest(JdbcTemplate jdbcTemplate) {
        if (ragConfigData.isForceRebuild()) {
            log.info("force-build=true -> truncating {}", ragVectorStoreTableName);
            jdbcTemplate.update("TRUNCATE TABLE " + ragVectorStoreTableName);
        } else {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + ragVectorStoreTableName, Integer.class);
            if (count != null && count > 0) {
                log.info("Vector Store already populated ({} rows). Skipping ingest. Set app.rag.force-rebuild=true to rebuild", count);
                return true;
            }
        }
        return false;
    }
    private Resource[] getResources() {
        var resolver = new PathMatchingResourcePatternResolver();
        String pdfPath = ragConfigData.getPdf().getPath();
        try {
            Resource[] pdfResources = resolver.getResources(pdfPath);
            if(pdfResources.length == 0) {
                log.warn("No pdfs found at {}", pdfPath);
                return null;
            }
            return pdfResources;
        } catch (IOException e) {
            throw new RagException("Could not get pdf resources!", e);
        }

    }

    private void ingestDocumentChunksToVectoreStore(Resource[] pdfResources) {
        var documents = getDocuments(pdfResources);
        var chunks = getChunks(documents);
        addChunkIndex(chunks);
        vectorStore.add(chunks);
        log.info("Ingest {} chunks into PgVectorStore", chunks.size());
    }

    private List<Document> getDocuments(Resource[] pdfResources) {
        List<Document> documents = new ArrayList<>();
        for (Resource resource: pdfResources) {
            List<Document> parts = getDocumentParts(resource);
            addMetadata(resource, parts);
            documents.addAll(parts);
        }
        return documents;
    }

    private List<Document> getDocumentParts(Resource pdfResource) {
        List<Document> parts;
        if(RagConstants.PARAGRAPH.equalsIgnoreCase(ragConfigData.getPdf().getMode())) {
            //Paragraph mode relies on PDF Outline/TOC; not all PDFS have it.
            parts = new ParagraphPdfDocumentReader(pdfResource, pdfDocumentReaderConfig).read();
        } else {
            parts = new PagePdfDocumentReader(pdfResource, pdfDocumentReaderConfig).read();
        }
        return parts;
    }
    private void addMetadata(Resource pdfResource, List<Document> parts) {
        for (var part: parts) {
            part.getMetadata().putIfAbsent(RagConstants.SOURCE, pdfResource.getFilename());
            part.getMetadata().putIfAbsent(RagConstants.DOC_TYPE,
                    pdfResource.getFilename().substring(0, pdfResource.getFilename().indexOf(".")));
            part.getMetadata().putIfAbsent(RagConstants.UPDATED_AT, ZonedDateTime.now().toLocalDate().toString());
        }
    }

    private List<Document> getChunks(List<Document> documents) {
        return tokenTextSplitter.apply(documents);

    }

    private void addChunkIndex(List<Document> chunks) {
        Map<String, Integer> counters = new HashMap<>();
        for (var chunk: chunks) {
            var source = String.valueOf(chunk.getMetadata().getOrDefault(RagConstants.SOURCE, RagConstants.UNKNOWN));
            var index = counters.merge(source, 1, Integer::sum) -1;
            chunk.getMetadata().put(RagConstants.CHUNK_INDEX, index);
        }
    }
}
