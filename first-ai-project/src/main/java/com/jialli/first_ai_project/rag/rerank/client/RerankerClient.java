package com.jialli.first_ai_project.rag.rerank.client;

import com.jialli.first_ai_project.rag.rerank.exception.RerankException;

import java.util.List;

public interface RerankerClient {
    double[] score(String query, List<String> documents) throws RerankException;
}
