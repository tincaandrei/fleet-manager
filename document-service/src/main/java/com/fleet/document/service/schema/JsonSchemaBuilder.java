package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public final class JsonSchemaBuilder {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private JsonSchemaBuilder() {
    }

    public static ObjectNode objectSchema(Map<String, ObjectNode> properties) {
        ObjectNode schema = JSON.objectNode();
        schema.put("type", "object");
        ObjectNode propertyNode = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");
        properties.forEach((name, property) -> {
            propertyNode.set(name, property);
            required.add(name);
        });
        schema.put("additionalProperties", false);
        return schema;
    }

    public static ObjectNode nullableString() {
        return nullableType("string");
    }

    public static ObjectNode nullableDate() {
        ObjectNode node = nullableString();
        node.put("description", "ISO date in yyyy-MM-dd format, or null when missing or uncertain.");
        return node;
    }

    public static ObjectNode nullableNumber() {
        return nullableType("number");
    }

    public static ObjectNode nullableConfidence() {
        ObjectNode node = nullableNumber();
        node.put("minimum", 0);
        node.put("maximum", 1);
        return node;
    }

    public static ObjectNode nullableStringEnum(List<String> values) {
        ObjectNode node = nullableString();
        ArrayNode enumValues = node.putArray("enum");
        values.forEach(enumValues::add);
        enumValues.addNull();
        return node;
    }

    public static ObjectNode stringArray() {
        ObjectNode node = JSON.objectNode();
        ArrayNode types = node.putArray("type");
        types.add("array");
        types.add("null");
        node.set("items", JSON.objectNode().put("type", "string"));
        return node;
    }

    public static ObjectNode invoiceItemsArray() {
        ObjectNode item = objectSchema(Map.of(
                "description", nullableString(),
                "quantity", nullableNumber(),
                "unit", nullableString(),
                "unitPrice", nullableNumber(),
                "totalPrice", nullableNumber()
        ));
        ObjectNode node = JSON.objectNode();
        ArrayNode types = node.putArray("type");
        types.add("array");
        types.add("null");
        node.set("items", item);
        return node;
    }

    private static ObjectNode nullableType(String type) {
        ObjectNode node = JSON.objectNode();
        ArrayNode types = node.putArray("type");
        types.add(type);
        types.add("null");
        return node;
    }
}
