package com.swedbank.bankapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI swedbankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Swedbank Bank API")
                        .description("Multi-currency banking REST API — Swedbank Tech Challenge")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Software Engineer")
                                .email("software-engineer@email.com")));
    }
}
