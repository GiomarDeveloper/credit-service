package com.bank.credit.exception;

import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Manejador global de excepciones para la aplicación de créditos.
 * <p>
 * Esta clase captura y maneja todas las excepciones no controladas en los controladores,
 * proporcionando respuestas de error consistentes y apropiadas para el cliente.
 * Utiliza el patrón ControllerAdvice de Spring para aplicar el manejo de excepciones
 * de manera global en toda la aplicación.
 * </p>
 *
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Maneja excepciones de recursos no encontrados.
   *
   * @param ex la excepción ResourceNotFoundException lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con estado 404 Not Found
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleNotFound(ResourceNotFoundException ex,
                                                            ServerWebExchange exchange) {
    log.warn("Resource not found: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.builder()
      .status(HttpStatus.NOT_FOUND.value())
      .error(HttpStatus.NOT_FOUND.getReasonPhrase())
      .message(ex.getMessage())
      .path(exchange.getRequest().getPath().value())
      .build();

    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
  }

  /**
   * Maneja excepciones de estado de respuesta específicas de Spring WebFlux.
   *
   * @param ex la excepción ResponseStatusException lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con el estado específico
   */
  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleResponseStatus(ResponseStatusException ex,
                                                                  ServerWebExchange exchange) {
    log.warn("Response status exception: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.builder()
      .status(ex.getStatus().value())
      .error(ex.getStatus().getReasonPhrase())
      .message(ex.getReason())
      .path(exchange.getRequest().getPath().value())
      .build();

    return Mono.just(ResponseEntity.status(ex.getStatus()).body(response));
  }

  /**
   * Maneja violaciones de reglas de negocio mediante IllegalArgumentException.
   *
   * @param ex la excepción IllegalArgumentException lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con estado 400 Bad Request
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex,
                                                                   ServerWebExchange exchange) {
    log.warn("Business rule violation: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.builder()
      .status(HttpStatus.BAD_REQUEST.value())
      .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
      .message(ex.getMessage())
      .path(exchange.getRequest().getPath().value())
      .build();

    return Mono.just(ResponseEntity.badRequest().body(response));
  }

  /**
   * Maneja errores de validación de datos en solicitudes de enlace web.
   *
   * @param ex la excepción WebExchangeBindException lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con detalles de validación
   */
  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex,
                                                                       ServerWebExchange exchange) {
    log.warn("Validation error: {}", ex.getMessage());

    Map<String, String> fieldErrors = ex.getFieldErrors().stream()
      .collect(Collectors.toMap(
        fieldError -> fieldError.getField(),
        fieldError -> fieldError.getDefaultMessage(),
        (msg1, msg2) -> msg1
      ));

    ErrorResponse response = ErrorResponse.builder()
      .status(HttpStatus.BAD_REQUEST.value())
      .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
      .message("Validation error in request fields")
      .path(exchange.getRequest().getPath().value())
      .details(fieldErrors)
      .build();

    return Mono.just(ResponseEntity.badRequest().body(response));
  }

  /**
   * Maneja violaciones de constraints de validación Bean Validation.
   *
   * @param ex la excepción ConstraintViolationException lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con detalles de constraints
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolation(
    ConstraintViolationException ex, ServerWebExchange exchange) {
    log.warn("Constraint violation: {}", ex.getMessage());

    Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
      .collect(Collectors.toMap(
        violation -> violation.getPropertyPath().toString(),
        violation -> violation.getMessage(),
        (msg1, msg2) -> msg1
      ));

    ErrorResponse response = ErrorResponse.builder()
      .status(HttpStatus.BAD_REQUEST.value())
      .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
      .message("Constraint violation in request")
      .path(exchange.getRequest().getPath().value())
      .details(fieldErrors)
      .build();

    return Mono.just(ResponseEntity.badRequest().body(response));
  }

  /**
   * Maneja excepciones runtime no específicas.
   *
   * @param ex la excepción RuntimeException lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con estado 500 Internal Server Error
   */
  @ExceptionHandler(RuntimeException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleRuntimeException(RuntimeException ex,
                                                                    ServerWebExchange exchange) {
    log.error("Runtime error: ", ex);

    ErrorResponse response = ErrorResponse.builder()
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .message(ex.getMessage())
      .path(exchange.getRequest().getPath().value())
      .build();

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
  }

  /**
   * Maneja cualquier excepción no capturada por los manejadores específicos.
   * <p>
   * Este método actúa como un catch-all para asegurar que todas las excepciones
   * sean manejadas y se devuelva una respuesta apropiada al cliente.
   * </p>
   *
   * @param ex la excepción general lanzada
   * @param exchange el contexto del intercambio web
   * @return Mono con ResponseEntity conteniendo ErrorResponse con estado 500 Internal Server Error
   */
  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ErrorResponse>> handleGeneralError(Exception ex,
                                                                ServerWebExchange exchange) {
    log.error("Unexpected error: ", ex);

    ErrorResponse response = ErrorResponse.builder()
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .message("An unexpected error occurred")
      .path(exchange.getRequest().getPath().value())
      .build();

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
  }
}