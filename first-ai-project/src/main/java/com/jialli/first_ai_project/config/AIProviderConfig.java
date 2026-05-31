package com.jialli.first_ai_project.config;

import com.jialli.first_ai_project.chat.advisor.ErrorWrappingAdvisor;
import com.jialli.first_ai_project.chat.advisor.SystemPromptAdvisor;
import com.jialli.first_ai_project.chat.advisor.ValidationAdvisor;
import com.jialli.first_ai_project.chat.openai.jailbreak.demo.BankingTools;
import com.jialli.first_ai_project.rag.config.data.PgVectorStoreConfigData;
import com.jialli.first_ai_project.rag.config.data.RagConfigData;
import com.jialli.first_ai_project.rag.config.postprocessor.CitationHeaderPostProcessor;
import com.jialli.first_ai_project.rag.config.postprocessor.NeighbourStitchPostProcessor;
import com.jialli.first_ai_project.rag.config.preprocessor.DomainSynonymTransformer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
public class AIProviderConfig {
    @Value("classpath:/templates/vector-store-memory-system-prompt.st")
    private Resource vectorStoreMemorySystemPrompt;

    @Value("classpath:/templates/prompt-chat-memory-system-prompt.st")
    private Resource promptChatMemorySystemPrompt;

    @Value("classpath:/templates/query-expander-prompt.st")
    private Resource queryExpanderPrompt;

    private static final int TOP_K = 10;
    private static final int MAX_MESSAGES = 5;
// @Qualifier("googleGenAiTextEmbedding")
    @Bean("chatMemoryVectorStore")
    public VectorStore chatMemoryVectorStore(JdbcTemplate jdbcTemplate,
                                             PgVectorStoreConfigData pgVectorStoreConfigData,
                                             @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForChatMemory())
                //following configurations are optional, as they are default config for vector store
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }

    @Bean("ragVectorStore")
    public VectorStore ragVectorStore(JdbcTemplate jdbcTemplate,
                                             PgVectorStoreConfigData pgVectorStoreConfigData,
                                             @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForRag())
                //following configurations are optional, as they are default config for vector store
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }
    @Bean("queryExpanderChatClientBuilder")
    ChatClient.Builder queryExpanderChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.0) //deterministic rewrite
                        .maxTokens(256) // short paraphrases
                        .build());
    }

    @Bean
    RetrievalAugmentationAdvisor ragAdvisor(@Qualifier("ragVectorStore") VectorStore vectorStore,
                                            RagConfigData ragConfigData,
                                            DomainSynonymTransformer domainSynonymTransformer,
                                            NeighbourStitchPostProcessor neighbourStitchPostProcessor,
                                            CitationHeaderPostProcessor citationHeaderPostProcessor,
                                            @Qualifier("queryExpanderChatClientBuilder") ChatClient.Builder queryExpanderChatClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(domainSynonymTransformer)
                .queryExpander(MultiQueryExpander.builder()
                        .chatClientBuilder(queryExpanderChatClientBuilder)
                        .numberOfQueries(ragConfigData.getQueryExpander().getNumberOfQueries())
                        .promptTemplate(new PromptTemplate(queryExpanderPrompt))
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(ragConfigData.getTopK())
                        .similarityThreshold(ragConfigData.getSimilarityThreshold())
                        .build())
                .documentPostProcessors(neighbourStitchPostProcessor, citationHeaderPostProcessor)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
                        .build())
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter(RagConfigData ragConfigData) {
        return TokenTextSplitter.builder()
                .withChunkSize(ragConfigData.getChunk().getSize())
                .withMinChunkSizeChars(ragConfigData.getChunk().getMinChunkSize())
                .withMinChunkLengthToEmbed(ragConfigData.getChunk().getMinChunkToEmbed())
                .withMaxNumChunks(ragConfigData.getChunk().getMaxNumChunks())
                .withKeepSeparator(ragConfigData.getChunk().isKeepSeparator())
                .build();
    }

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }
    @Bean("geminiChatClient")
    //@Bean
    //@ConditionalOnProperty(prefix="app.ai", name="provider", havingValue="google", matchIfMissing = true)
    ChatClient geminiChatClient(GoogleGenAiChatModel genAiChatModel,
                                SimpleLoggerAdvisor simpleLoggerAdvisor,
                                SafeGuardAdvisor safeGuardAdvisor,
                                ErrorWrappingAdvisor errorWrappingAdvisor,
                                SystemPromptAdvisor systemPromptAdvisor,
                                ValidationAdvisor validationAdvisor) {
        return ChatClient.builder(genAiChatModel)
                .defaultAdvisors(safeGuardAdvisor, simpleLoggerAdvisor, errorWrappingAdvisor, systemPromptAdvisor, validationAdvisor)
                .build();
    }


    @Bean("geminiGeneralChatClient")
        //@Bean
        //@ConditionalOnProperty(prefix="app.ai", name="provider", havingValue="google", matchIfMissing = true)
    ChatClient geminiGeneralChatClient(GoogleGenAiChatModel genAiChatModel,
                                       BankingTools bankingTools,
                                       SimpleLoggerAdvisor simpleLoggerAdvisor,
                                       SystemPromptAdvisor systemPromptAdvisor) {
        return ChatClient.builder(genAiChatModel)
                //.defaultTools(bankingTools)
                .defaultAdvisors(simpleLoggerAdvisor)
                .build();
    }
    //@Bean
    //@ConditionalOnProperty(prefix="app.ai", name="provider", havingValue="openai", matchIfMissing = true)
    @Bean("openAiChatClient")
    ChatClient openAiChatClient(OpenAiChatModel openAiChatModel,
                                SimpleLoggerAdvisor simpleLoggerAdvisor,
                                SafeGuardAdvisor safeGuardAdvisor,
                                ErrorWrappingAdvisor errorWrappingAdvisor,
                                SystemPromptAdvisor systemPromptAdvisor) {
        //ChatOptions chatOptions = ChatOptions.builder().build();
        //return ChatClient.builder(openAiChatModel).defaultOptions(chatOptions).build();
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(safeGuardAdvisor, simpleLoggerAdvisor, errorWrappingAdvisor, systemPromptAdvisor)
                .build();
    }

    @Bean("geminiChatClientWithMemory")
    ChatClient geminiChatClientWithMemory(GoogleGenAiChatModel googleGenAiChatModel,
                                          ChatMemory chatMemory,
                                          @Qualifier("chatMemoryVectorStore") VectorStore pgVectorStore,
                                          SimpleLoggerAdvisor simpleLoggerAdvisor) {

        return ChatClient.builder(googleGenAiChatModel)
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory)
                        .systemPromptTemplate(new PromptTemplate(promptChatMemorySystemPrompt))
                        .build())
//                .defaultAdvisors(simpleLoggerAdvisor, VectorStoreChatMemoryAdvisor.builder(pgVectorStore)
//                        .systemPromptTemplate(new PromptTemplate(vectorStoreMemorySystemPrompt))
//                        .defaultTopK(TOP_K)
//                        .build(), MessageChatMemoryAdvisor.builder(chatMemory).build())
//                //.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
    @Bean("openAIChatClientWithMemory")
    ChatClient openAIChatClientWithMemory(OpenAiChatModel openAiChatModel,
                                          ChatMemory chatMemory,
                                          @Qualifier("chatMemoryVectorStore")  VectorStore pgVectorStore,
                                          SimpleLoggerAdvisor simpleLoggerAdvisor) {

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory)
               //         .systemPromptTemplate(new PromptTemplate(promptChatMemorySystemPrompt))
                        .build(),
                        simpleLoggerAdvisor,
                        VectorStoreChatMemoryAdvisor.builder(pgVectorStore)
                        .systemPromptTemplate(new PromptTemplate(vectorStoreMemorySystemPrompt))
                        .defaultTopK(TOP_K)
                        .build())
//                .defaultAdvisors(simpleLoggerAdvisor, VectorStoreChatMemoryAdvisor.builder(pgVectorStore)
//                        .systemPromptTemplate(new PromptTemplate(vectorStoreMemorySystemPrompt))
//                        .defaultTopK(TOP_K)
//                        .build())
                //  .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
    @Bean("openAiGeneralChatClient")
    ChatClient openAiGeneralChatClient(OpenAiChatModel openAiChatModel,
                                       SimpleLoggerAdvisor simpleLoggerAdvisor,
                                       SystemPromptAdvisor systemPromptAdvisor,
                                       BankingTools bankingTools) {
        //ChatOptions chatOptions = ChatOptions.builder().build();
        //return ChatClient.builder(openAiChatModel).defaultOptions(chatOptions).build();
        return ChatClient.builder(openAiChatModel)
                .defaultTools(bankingTools)
                .defaultAdvisors(simpleLoggerAdvisor)
                .build();
    }

    //@Bean
    //@ConditionalOnProperty(prefix="app.ai", name="provider", havingValue="vertexai", matchIfMissing = true)
    @Bean("vertexAiChatClient")
    ChatClient vertexAIchatclient(VertexAiGeminiChatModel vertexAiGeminiChatModel) {
        return ChatClient.builder(vertexAiGeminiChatModel).build();
    }
    @Bean("huggingFaceChatClient")
    ChatClient huggingFaceChatClient(HuggingfaceChatModel huggingfaceChatModel) {
        return ChatClient.builder(huggingfaceChatModel).build();
    }

    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }
    @Bean("openAiRagChatClient")
    ChatClient openAiRagChatclient(OpenAiChatModel openAiChatModel,
                                   SimpleVectorStore simpleVectorStore,
                                   SimpleLoggerAdvisor simpleLoggerAdvisor,
                                   RagConfigData ragConfigData) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(simpleVectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(ragConfigData.getTopK())
                                .similarityThreshold(ragConfigData.getSimilarityThreshold())
                                .build()).build(), simpleLoggerAdvisor)
                .build();

    }
    @Bean("openAiAdvancedRagChatClient")
    ChatClient openAiAdvancedRagChatClient(OpenAiChatModel openAiChatModel,
                                           RetrievalAugmentationAdvisor retrievalAugmentationAdvisor){
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }
    @Bean
    @Primary
    EmbeddingModel primaryEmbedding(@Qualifier("openAiEmbeddingModel") EmbeddingModel delegateEmbeddingModel) {
        return delegateEmbeddingModel;
    }
    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor () {
        return new SimpleLoggerAdvisor();
    }
    @Bean
    SafeGuardAdvisor safeGuardAdvisor() {
        return new SafeGuardAdvisor(List.of(
                "password", "ssn", "credit card", "iban", "bank account",
                "api_key", "secret", "private_key", "token",
                "confidential", "classified", "internal only", "Ignore previous instructions",
                "Ignore instructions", "system prompt", "hack"));
    }

}
