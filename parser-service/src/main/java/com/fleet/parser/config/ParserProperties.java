package com.fleet.parser.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "parser")
public class ParserProperties {

    @NotBlank
    private String name = "ollama-document-parser";

    @NotBlank
    private String version = "1.0.0";

    @Min(1)
    private int minimumTextLength = 80;

    @Min(1)
    private int goodTextLength = 400;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getMinimumTextLength() {
        return minimumTextLength;
    }

    public void setMinimumTextLength(int minimumTextLength) {
        this.minimumTextLength = minimumTextLength;
    }

    public int getGoodTextLength() {
        return goodTextLength;
    }

    public void setGoodTextLength(int goodTextLength) {
        this.goodTextLength = goodTextLength;
    }
}
