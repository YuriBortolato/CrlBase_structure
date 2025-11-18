package com.apirest.api.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a todas as rotas da API

                // Libera o front-end rodando em localhost:5173
                .allowedOrigins("http://localhost:5173")

                // Libera os métodos que o front pode usar
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

               // Libera todos os headers que o front pode enviar
                .allowedHeaders("*")

                // Permite envio de cookies e credenciais
                .allowCredentials(true);
    }
}
