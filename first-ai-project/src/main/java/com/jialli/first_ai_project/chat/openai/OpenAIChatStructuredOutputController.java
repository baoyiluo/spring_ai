package com.jialli.first_ai_project.chat.openai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAIChatStructuredOutputController {
    private final ChatClient chatClient;
    public OpenAIChatStructuredOutputController(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    @PostMapping("/structured-list")
    public List<String> structuredList(@RequestBody String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .entity(new ListOutputConverter());
    }
    @PostMapping("/structured-map")
    public Map<String, Object> structuredMap(@RequestBody String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .entity(new MapOutputConverter());
    }

    @PostMapping("/general-chat")
    public String generalChat(@RequestBody String message) {
        ChatOptions chatOptions = ChatOptions.builder()
                .maxTokens(1000)
                //.temperature(2.0)
                //.topP(0.1) less random
                //.topP(0.9)
                .stopSequences(List.of("END_OF_PARAGRAPH"))
                .build();
        return chatClient.prompt()
                .options(chatOptions)
                .user(message)
                .call()
                .content();
    }

}
