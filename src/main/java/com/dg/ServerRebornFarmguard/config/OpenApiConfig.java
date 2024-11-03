package com.dg.ServerRebornFarmguard.config;



import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Info apiInfo = new io.swagger.v3.oas.models.info.Info()
                .title("Farmguard API Documentation")
                .version("1.0")
                .description("API системы СКУД, мой tg @ricooo_de, tel:+79255855441");

        return new OpenAPI()
                .info(apiInfo);
    }
}

