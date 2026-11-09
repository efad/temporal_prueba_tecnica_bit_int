package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import com.pruebatecnica.platform.resilience.ResilientCaller;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({ClientProperties.class, OpeningProperties.class})
public class OrchestratorConfig {

    @Bean
    RestClient accountRestClient(ObjectMapper objectMapper, ClientProperties properties) {
        return client(objectMapper, properties.accountServiceUrl());
    }

    @Bean
    RestClient cardRestClient(ObjectMapper objectMapper, ClientProperties properties) {
        return client(objectMapper, properties.cardServiceUrl());
    }

    @Bean
    ResilientCaller resilientCaller(CircuitBreakerRegistry circuitBreakers, BulkheadRegistry bulkheads) {
        return new ResilientCaller(circuitBreakers, bulkheads);
    }

    private RestClient client(ObjectMapper objectMapper, String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .messageConverters(converters -> converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper)))
                .build();
    }
}
