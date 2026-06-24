package com.fleet.document.service.ai;

public interface DocumentAiClient {

    AiExtractionResponse extract(AiExtractionRequest request);
}
