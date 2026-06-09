package com.jialli.first_ai_project;

import com.jialli.first_ai_project.rag.service.PdfWatcherService;
import com.jialli.first_ai_project.rag.service.RagIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FirstAiProjectApplication implements CommandLineRunner {
	private final RagIngestionService ragIngestionService;
	private  final PdfWatcherService pdfWatcherService;

    public FirstAiProjectApplication(RagIngestionService ragIngestionService,
									 PdfWatcherService pdfWatcherService) {
        this.ragIngestionService = ragIngestionService;
        this.pdfWatcherService = pdfWatcherService;
    }


    public static void main(String[] args) {
		SpringApplication.run(FirstAiProjectApplication.class, args);
	}


	@Override
	public void run(String... args) throws Exception {
		ragIngestionService.initializePgVectorStore();
		pdfWatcherService.start();
	}
}
