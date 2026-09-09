package com.robertosrjr.pedidos.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Pedidos API")
				.version("1.0.0")
				.description("POC Virtual Threads - Orders Management System")
				.contact(new Contact()
					.name("Roberto Silva")
					.url("https://github.com/robertosrjr"))
				.license(new License()
					.name("MIT")));
	}
}
