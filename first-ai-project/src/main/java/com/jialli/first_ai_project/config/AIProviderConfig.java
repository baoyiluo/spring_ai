package com.jialli.first_ai_project.config;

import com.jialli.first_ai_project.chat.advisor.ErrorWrappingAdvisor;
import com.jialli.first_ai_project.chat.advisor.SystemPromptAdvisor;
import com.jialli.first_ai_project.chat.advisor.ValidationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AIProviderConfig {
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
