package io.event.ems.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Event Management System (EMS) API",
        version = "1.0.0",
        description = "This API provides endpoints for managing events, users, and venues within the Event Management System (EMS). " +
                "It allows users to create, retrieve, update, and delete events, register and manage user accounts, " +
                "and maintain information about event locations. This API is intended for developers integrating with the EMS platform.",
        contact = @Contact(
                name = "EMS API Support Team",
                email = "support@example.com",
                url = "https://example.com/support"
        ),
        license = @License(
                name = "Apache 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        ),
        termsOfService = "https://example.com/api/terms"
    ),
    servers = {
        @Server(url = "https://api.example.com/v1", description = "Production Server"),
        @Server(url = "https://staging-api.example.com/v1", description = "Staging Server"),
        @Server(url = "http://localhost:8080", description = "Development Server")
    },
    externalDocs = @ExternalDocumentation(
            description = "Find out more about EMS API",
            url = "https://example.com/docs"
    )
)
public class OpenApiConfig {}