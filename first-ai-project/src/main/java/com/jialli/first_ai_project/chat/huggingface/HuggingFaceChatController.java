package com.jialli.first_ai_project.chat.huggingface;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/huggingface/chat")
public class HuggingFaceChatController {
    private final static String SYSTEM_PROMPT = "You are a senior engineer. Generate code based on the given description. " +
            "Ensure the coee is idiomatic, efficient, and follows best practices. ";
    private ChatClient chatClient;
    public HuggingFaceChatController(@Qualifier("huggingFaceChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    @PostMapping("/generate-code")
    public ChatClientResponse generateCode(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call().chatClientResponse();

    }
}
