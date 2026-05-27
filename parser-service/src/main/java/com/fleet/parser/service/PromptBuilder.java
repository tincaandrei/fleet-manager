package com.fleet.parser.service;

import com.fleet.parser.strategy.DocumentExtractionStrategy;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilder {

    public String buildPrompt(DocumentExtractionStrategy strategy, String extractedText) {
        return strategy.buildPrompt(extractedText);
    }
}
