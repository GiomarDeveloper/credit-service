package com.bank.credit.client;

import com.bank.credit.exception.ResourceNotFoundException;
import com.bank.credit.model.response.CustomerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Cliente para consumir servicios del microservicio de clientes.
 * <p>
 * Esta clase proporciona métodos para interactuar con el servicio de clientes
 * mediante WebClient reactivo, permitiendo operaciones asíncronas y no bloqueantes
 * para obtener información de clientes y sus tipos.
 * </p>
 * <p>
 * El cliente maneja automáticamente errores HTTP 4xx como ResourceNotFoundException
 * y errores 5xx como ResponseStatusException con estado SERVICE_UNAVAILABLE.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerServiceClient {

  private final WebClient webClient;

  @Value("${external.services.customer.url:http://localhost:8081}")
  private String customerServiceUrl;


  /**
   * Obtiene el tipo de cliente desde el servicio de clientes.
   * <p>
   * Realiza una llamada HTTP GET al servicio de clientes para recuperar
   * el tipo de cliente (ej: "PERSONAL", "BUSINESS", "VIP") asociado al
   * identificador proporcionado. Este método es útil para aplicar reglas
   * de negocio específicas según el tipo de cliente.
   * </p>
   *
   * </pre>
   */
  public Mono<String> getCustomerType(String customerId) {
    log.info("Getting customer type for: {}", customerId);

    return webClient.get()
      .uri(customerServiceUrl + "/customers/{customerId}", customerId)
      .retrieve()
      .onStatus(status -> status.is4xxClientError(), response ->
        Mono.error(new ResourceNotFoundException("Customer not found with id: " + customerId))
      )
      .onStatus(status -> status.is5xxServerError(), response ->
        Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Customer service unavailable"))
      )
      .bodyToMono(CustomerResponse.class)
      .map(CustomerResponse::getCustomerType)
      .doOnSuccess(type -> log.debug("Customer {} type: {}", customerId, type))
      .doOnError(error -> log.error("Error getting customer type for {}: {}", customerId,
        error.getMessage()));
  }

  /**
   * Obtiene la información completa de un cliente por su identificador.
   * <p>
   * Realiza una llamada HTTP GET al servicio de clientes para recuperar
   * todos los datos del cliente, incluyendo información personal, tipo de cliente
   * y cualquier otro atributo relevante definido en CustomerResponse.
   * </p>
   *
   * @param customerId el identificador único del cliente
   * @return Mono de CustomerResponse que contiene la información completa del cliente
   * @throws ResourceNotFoundException si no se encuentra el cliente con el ID proporcionado
   * @throws ResponseStatusException si el servicio de clientes no está disponible
   *
   * @see CustomerResponse
   * @see ResourceNotFoundException
   * @see ResponseStatusException
   */
  public Mono<CustomerResponse> getCustomerById(String customerId) {
    log.info("Getting customer by ID: {}", customerId);

    return webClient.get()
      .uri(customerServiceUrl + "/customers/{customerId}", customerId)
      .retrieve()
      .onStatus(status -> status.is4xxClientError(), response ->
        Mono.error(new ResourceNotFoundException("Customer not found with id: " + customerId))
      )
      .onStatus(status -> status.is5xxServerError(), response ->
        Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Customer service unavailable"))
      )
      .bodyToMono(CustomerResponse.class)
      .doOnSuccess(customer -> log.debug("Retrieved customer: {} {}", customer.getFirstName(),
        customer.getLastName()))
      .doOnError(
        error -> log.error("Error getting customer by ID {}: {}", customerId, error.getMessage()));
  }
}