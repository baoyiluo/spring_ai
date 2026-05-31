package com.jialli.first_ai_project.chat.openai.jailbreak.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BankingTools {
    @Tool(name="get-account-balance", description = "Get the current account balance for a given account Id")
    public String getAccountBalance(@ToolParam(description = "The account id to look up") String accountId) {
        if("12345".equals(accountId)) {
            return "$5000.00";
        }
        return "Account not found";
    }
}
