package com.jialli.first_ai_project.chat.gemini;

public class GeminiChatException extends  RuntimeException {
    public GeminiChatException(String message) {
        super(message);
    }
    public GeminiChatException(String message, Throwable cause) {
        super(message,cause);
    }
}
