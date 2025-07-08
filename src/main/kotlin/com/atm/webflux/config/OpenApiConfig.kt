package com.atm.webflux.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenApi(): OpenAPI {
        return OpenAPI()
            // API Information
            .info(
                Info().title("ATM Web Service API")
                    .description("A sample API for simulating ATM operations using Spring WebFlux.")
                    .version("1.0.0")
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0.html")
                    )
            )
            // Define servers (e.g., development, production)
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Development Server")
                )
            )
    }
}