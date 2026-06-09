package com.jialli.first_ai_project.rag.rerank.exception;

public class RerankException extends RuntimeException {
    public RerankException(String message) {
        super(message);
    }

    public RerankException(String message, Throwable cause) {
        super(message, cause);
    }
}
