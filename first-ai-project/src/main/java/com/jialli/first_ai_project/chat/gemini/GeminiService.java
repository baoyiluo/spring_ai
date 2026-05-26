package com.jialli.first_ai_project.chat.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

@Service
public class GeminiService {
    private static final String GEMINI_API_KEY = "GEMINI_API_KEY";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent";
    private static final String CONTENT_TYPE = "application/json";
    private final static String SYSTEM_PROMPT = "You are a helpful assistant that summarize any given content. " +
            "Ensure the summary is concise, informative, and captures the key points. " +
            "Use a friendly and approachable tone while maintaining professionalism." +
            "please don't answer not summarization questions, and reply I can only help with summarization tasks.";
    private final ObjectMapper objectMapper;
    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public String chat(String prompt) throws GeminiChatException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var request = getRequest(prompt);
            var response = httpClient.execute(request, resp -> EntityUtils.toString(resp.getEntity()));
            return parserResponse(response);
        } catch (IOException e) {
            throw new GeminiChatException("Could not call ");
        }
    }

    private HttpPost getRequest(String prompt) throws JsonProcessingException, UnsupportedEncodingException {
        var request = new HttpPost(GEMINI_API_URL);
        var geminiApiKey = System.getenv(GEMINI_API_KEY);
        request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        request.addHeader("x-goog-api-key", geminiApiKey);
        Map<String, Object> part = Map.of(
                "text", prompt
        );
        var parts = Arrays.asList(part);
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "parts", parts
        );
        Map<String, Object> modelPart = Map.of(
                "text", SYSTEM_PROMPT
        );
        var systemParts = Arrays.asList(modelPart);
        Map<String, Object> systemInstruction = Map.of(
                "parts", systemParts
        );
        var contents = Arrays.asList(userMessage);

        var requestBody = Map.of(
                "contents", contents,
                "systemInstruction", systemInstruction
        );
        String requestBodyStr = objectMapper.writeValueAsString(requestBody);
        request.setEntity(new StringEntity(requestBodyStr));
        return request;
    }
    private String parserResponse(String response) throws JsonProcessingException {
        Map<String, Object> genAiResponse = objectMapper.readValue(response, Map.class);
        return genAiResponse.get("candidates").toString();
    }
}
