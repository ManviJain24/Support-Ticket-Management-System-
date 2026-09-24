package com.ticketing.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public WebConfig(@Value("${app.frontend-origin:http://localhost:3000}") String frontendOrigin) {
        Set<String> origins = new LinkedHashSet<>();
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");
        origins.add("http://[::1]:*");
        Arrays.stream(frontendOrigin.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .forEach(origins::add);
        this.allowedOriginPatterns = origins.toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .maxAge(3600);
    }
}
