package com.pruebatecnica.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pruebatecnica.platform.web.CorrelationFilter;
import com.pruebatecnica.platform.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class PlatformAutoConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    FilterRegistrationBean<CorrelationFilter> correlationFilter(Environment environment) {
        boolean simulationEnabled = environment.getProperty("app.simulation.enabled", Boolean.class, true);
        FilterRegistrationBean<CorrelationFilter> registration =
                new FilterRegistrationBean<>(new CorrelationFilter(simulationEnabled));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
