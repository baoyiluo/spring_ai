package com.jialli.first_ai_project.chat.gemini.dto.response;

import java.util.List;

public record SummarizationResponse(
    List<String> actionItems,
    List<String> decisions,
    String errorMessage) {
}

