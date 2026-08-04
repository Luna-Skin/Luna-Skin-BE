package com.luna.skin.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "luna-skin.storage.local")
public record StorageProperties(String baseDir, String baseUrl) {
}