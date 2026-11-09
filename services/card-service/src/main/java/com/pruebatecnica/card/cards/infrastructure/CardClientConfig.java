package com.pruebatecnica.card.cards.infrastructure;

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
@EnableConfigurationProperties(AccountClientProperties.class)
public class CardClientConfig {

    @Bean
    RestClient accountRestClient(ObjectMapper objectMapper, AccountClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(properties.accountServiceUrl())
                .requestFactory(factory)
                .messageConverters(converters -> converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper)))
                .build();
    }

    @Bean
    ResilientCaller resilientCaller(CircuitBreakerRegistry circuitBreakers, BulkheadRegistry bulkheads) {
        return new ResilientCaller(circuitBreakers, bulkheads);
    }
}
