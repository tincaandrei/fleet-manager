package com.fleet.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        boolean stream,
        String format,
        List<Message> messages
) {
    public record Message(String role, String content) {
    }
}
