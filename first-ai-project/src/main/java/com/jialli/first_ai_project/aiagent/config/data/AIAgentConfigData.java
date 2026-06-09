package com.jialli.first_ai_project.aiagent.config.data;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties("app.agent")
public class AIAgentConfigData {
    private String uploadDir;
    private DiagramToolProperties diagramTool;

    public static class DiagramToolProperties {
        private Double temperature;
    }
}
