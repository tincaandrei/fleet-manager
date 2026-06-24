package com.fleet.document.service;

import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.service.parsing.DocumentParsingOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentParsingService {

    private final DocumentParsingOrchestrator orchestrator;

    public ParserResultRequest parse(VehicleDocument document) {
        return orchestrator.parse(document);
    }
}
