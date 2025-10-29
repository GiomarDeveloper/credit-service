package com.bank.credit.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Respuesta de error estándar para la API de créditos.
 * <p>
 * Esta clase representa una estructura consistente para todas las respuestas de error
 * devueltas por la API. Incluye información detallada sobre el error, timestamp,
 * código de estado HTTP y detalles específicos de validación cuando corresponda.
 * </p>
 *
 */
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