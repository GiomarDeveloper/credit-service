package com.bank.credit.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@Schema(description = "Respuesta de error estándar de la API")
public class ErrorResponse {
    @Builder.Default
    private Instant timestamp = Instant.now();
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details;
}