package com.example.supportassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpConfig {

    @Bean
    RestClient restClient(
            RestClient.Builder builder,
            @Value("${app.llm.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.llm.read-timeout-ms:15000}") int readTimeoutMs) {

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return builder.requestFactory(factory).build();
    }
}
