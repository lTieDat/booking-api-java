package com.example.bookingapi.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BookingAPI",
                version = "v1",
                description = "REST API for booking hotels, rooms, bookings and authentication. "
                        + "Local/dev default manager accounts: admin@booking.local / admin123 and manager@booking.local / admin123. "
                        + "Local/dev default receptionist account: reception@booking.local / admin123. "
                        + "Use POST /api/auth/manager/signin for manager/admin JWTs and POST /api/auth/signin for receptionist JWTs.",
                contact = @Contact(name = "BookingAPI Team")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer reusableResponseComponents() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            components.addResponses("BadRequest", errorResponse(
                    "Bad Request - invalid request body, query parameter, path variable, or business validation failure.",
                    400,
                    "Bad Request",
                    "Request validation failed"
            ));
            components.addResponses("Unauthorized", errorResponse(
                    "Unauthorized - missing, expired, malformed JWT, or invalid login credentials.",
                    401,
                    "Unauthorized",
                    "Unauthorized"
            ));
            components.addResponses("Forbidden", errorResponse(
                    "Forbidden - authenticated account does not have permission to perform this operation.",
                    403,
                    "Forbidden",
                    "Access denied"
            ));
            components.addResponses("NotFound", errorResponse(
                    "Not Found - requested resource does not exist.",
                    404,
                    "Not Found",
                    "Resource not found"
            ));
            components.addResponses("InternalServerError", errorResponse(
                    "Internal Server Error - unexpected server, database, or object storage failure.",
                    500,
                    "Internal Server Error",
                    "Unexpected server error"
            ));
        };
    }

    private ApiResponse errorResponse(String description, int status, String error, String message) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .schema(new ObjectSchema()
                                        .addProperty("success", new io.swagger.v3.oas.models.media.BooleanSchema().example(false))
                                        .addProperty("status", new IntegerSchema().example(status))
                                        .addProperty("error", new StringSchema().example(error))
                                        .addProperty("message", new StringSchema().example(message))
                                        .addProperty("path", new StringSchema().example("/api/resource"))
                                        .addProperty("validationErrors", new ObjectSchema().nullable(true)))
                                .addExamples("default", new Example().value("""
                                        {
                                          "success": false,
                                          "status": %d,
                                          "error": "%s",
                                          "message": "%s",
                                          "path": "/api/resource",
                                          "validationErrors": null
                                        }
                                        """.formatted(status, error, message)))));
    }
}
