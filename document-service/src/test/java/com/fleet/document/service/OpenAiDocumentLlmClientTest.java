package com.fleet.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.dto.LlmUsageDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiDocumentLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsResponsesRequestAndMapsUsage() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiDocumentLlmClient client = new OpenAiDocumentLlmClient(
                objectMapper,
                builder.build(),
                "gpt-5.4-mini",
                1500,
                0
        );
        server.expect(requestTo("/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.4-mini"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andRespond(withSuccess("""
                        {
                          "id": "resp_123",
                          "model": "gpt-5.4-mini",
                          "output": [
                            {
                              "type": "message",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "{\\"licensePlate\\":\\"B123ABC\\",\\"llmConfidence\\":0.82}"
                                }
                              ]
                            }
                          ],
                          "usage": {
                            "input_tokens": 120,
                            "output_tokens": 40,
                            "total_tokens": 160
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DocumentLlmClient.LlmExtractionResult result = client.extractJson("Extract JSON", expectedSchema());

        assertThat(result.json().path("licensePlate").asText()).isEqualTo("B123ABC");
        assertThat(result.usage()).isEqualTo(new LlmUsageDto("openai", "gpt-5.4-mini", "resp_123", 120, 40, 160));
        server.verify();
    }

    @Test
    void mapsRateLimitToDedicatedErrorCode() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiDocumentLlmClient client = new OpenAiDocumentLlmClient(
                objectMapper,
                builder.build(),
                "gpt-5.4-mini",
                1500,
                0
        );
        server.expect(requestTo("/responses"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.extractJson("Extract JSON", expectedSchema()))
                .isInstanceOf(DocumentLlmException.class)
                .extracting("errorCode")
                .isEqualTo("OPENAI_RATE_LIMITED");
        server.verify();
    }

    private ObjectNode expectedSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("licensePlate", "string|null");
        schema.put("llmConfidence", "number|null");
        return schema;
    }
}
