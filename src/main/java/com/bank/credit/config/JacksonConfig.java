package com.bank.credit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Jackson para la serialización y deserialización JSON.
 * <p>
 * Esta clase configura el ObjectMapper de Jackson para soportar correctamente
 * los tipos de Java Time API (java.time) mediante el registro del módulo JavaTimeModule.
 * </p>
 * <p>
 * La configuración asegura que las fechas y horas sean serializadas y deserializadas
 * correctamente en formato ISO-8601.
 * </p>
 */
@Configuration
public class JacksonConfig {

  /**
   * Configura y provee una instancia de ObjectMapper con soporte para Java Time API.
   * <p>
   * El ObjectMapper configurado incluye el módulo JavaTimeModule que permite
   * el manejo correcto de tipos como LocalDate, LocalDateTime, ZonedDateTime, etc.
   * </p>
   *
   * @return ObjectMapper configurado con soporte para Java Time API
   *
   * @see ObjectMapper
   * @see JavaTimeModule#JavaTimeModule()
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}