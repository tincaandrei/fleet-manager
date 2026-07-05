package com.fleet.document.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMqProperties {

    private boolean enabled = true;
    private String exchange = "doccufleet.documents";
    private String parsingQueue = "doccufleet.document.parsing";
    private String parsingRoutingKey = "document.parse";
    private String deadLetterExchange = "doccufleet.documents.dlx";
    private String deadLetterQueue = "doccufleet.document.parsing.dlq";
    private String deadLetterRoutingKey = "document.parse.failed";
    private long publisherConfirmTimeoutMs = 5000;
}
