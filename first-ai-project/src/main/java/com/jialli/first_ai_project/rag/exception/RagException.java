package com.jialli.first_ai_project.rag.exception;

public class RagException extends  RuntimeException {
    public RagException(String message) {
        super(message);
    }

    public RagException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
