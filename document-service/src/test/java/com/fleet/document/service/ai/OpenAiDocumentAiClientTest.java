package com.fleet.document.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.dto.LlmUsageDto;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.service.schema.RcaExtractionSchema;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiDocumentAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsResponsesRequestUsingProvidedSchemaAndMapsUsage() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiDocumentAiClient client = new OpenAiDocumentAiClient(
                objectMapper,
                builder.build(),
                "gpt-5.4-mini",
                1500,
                0
        );
        ObjectNode schema = new RcaExtractionSchema().jsonSchema();
        server.expect(requestTo("/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.4-mini"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.text.format.schema.properties.licensePlate.type[0]").value("string"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("string|null"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("yyyy-MM-dd|null"))))
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

        AiExtractionResponse result = client.extract(new AiExtractionRequest(
                "Document text",
                TextExtractionMethod.PDFBOX,
                DocumentType.INSURANCE,
                "RCA",
                schema,
                "Extract RCA data"
        ));

        assertThat(result.extractedData().path("licensePlate").asText()).isEqualTo("B123ABC");
        assertThat(result.usage()).isEqualTo(new LlmUsageDto("openai", "gpt-5.4-mini", "resp_123", 120, 40, 160));
        server.verify();
    }

    @Test
    void mapsRateLimitToDedicatedErrorCode() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiDocumentAiClient client = new OpenAiDocumentAiClient(
                objectMapper,
                builder.build(),
                "gpt-5.4-mini",
                1500,
                0
        );
        server.expect(requestTo("/responses"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.extract(new AiExtractionRequest(
                "Document text",
                TextExtractionMethod.PDFBOX,
                DocumentType.INSURANCE,
                "RCA",
                new RcaExtractionSchema().jsonSchema(),
                "Extract RCA data"
        )))
                .isInstanceOf(DocumentAiException.class)
                .extracting("errorCode")
                .isEqualTo("OPENAI_RATE_LIMITED");
        server.verify();
    }
}
