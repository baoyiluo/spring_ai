package com.jialli.first_ai_project.chat.memory.dto;

public record Order(String orderId, String carrier, OrderStatus status,
                     String userId, String userName) {

}
