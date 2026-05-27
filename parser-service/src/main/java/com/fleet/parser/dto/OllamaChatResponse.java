package com.fleet.parser.dto;

public record OllamaChatResponse(
        Message message,
        Boolean done
) {
    public record Message(String role, String content) {
    }
}
