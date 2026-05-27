package com.fleet.parser.service;

import com.fleet.parser.config.InternalApiKeyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class InternalApiKeyService {

    private final InternalApiKeyProperties properties;

    public boolean isValid(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        byte[] expected = properties.getApiKey().getBytes(StandardCharsets.UTF_8);
        byte[] actual = candidate.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
