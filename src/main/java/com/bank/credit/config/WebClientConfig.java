package com.bank.credit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración de WebClient para llamadas HTTP reactivas.
 * <p>
 * Esta clase proporciona la configuración básica del WebClient utilizado
 * para realizar llamadas HTTP no bloqueantes a otros microservicios.
 * </p>
 * <p>
 * El WebClient configurado aquí es utilizado por los diferentes clientes
 * de servicio (AccountServiceClient, CustomerServiceClient, etc.) para
 * comunicarse con sus respectivos microservicios.
 * </p>
 */
@Configuration
public class WebClientConfig {

  /**
   * Crea y configura una instancia de WebClient.
   * <p>
   * Provee un WebClient con configuración básica que puede ser utilizada
   * para realizar llamadas HTTP reactivas. Los detalles específicos de
   * cada servicio (URL base, headers, etc.) son configurados en los
   * clientes individuales.
   * </p>
   *
   * @return WebClient instancia configurada para llamadas HTTP reactivas
   *
   * @see WebClient
   * @see WebClient.Builder#build()
   */
  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }
}