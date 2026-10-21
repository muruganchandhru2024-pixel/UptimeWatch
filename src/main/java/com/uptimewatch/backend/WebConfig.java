
package com.uptimewatch.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
    "http://localhost:5173",
    "https://main.d20zbdaba3e9ew.amplifyapp.com"
)
                    .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
