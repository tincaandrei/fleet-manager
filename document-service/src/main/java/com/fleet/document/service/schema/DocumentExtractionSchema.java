package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;

import java.util.List;

public interface DocumentExtractionSchema {

    DocumentType documentType();

    String subtype();

    ObjectNode jsonSchema();

    List<String> importantFields();

    boolean requiresVehicleIdentifier();

    List<String> relevantFields();

    String promptInstructions();
}
