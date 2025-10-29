package com.bank.credit.client;

import com.bank.credit.exception.ResourceNotFoundException;
import com.bank.credit.model.response.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cliente para consumir servicios del microservicio de cuentas.
 * <p>
 * Esta clase proporciona métodos para interactuar con el servicio de cuentas
 * mediante WebClient reactivo, permitiendo operaciones asíncronas y no bloqueantes
 * para obtener información de cuentas, saldos y validaciones.
 * </p>
 *
 * @see WebClient
 * @see Mono
 * @see Flux
 * @see Component
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

  private final WebClient webClient;

  @Value("${external.services.account.url:http://localhost:8082}")
  private String accountServiceUrl;

  /**
   * Obtiene todas las cuentas asociadas a un cliente específico.
   * <p>
   * Realiza una llamada HTTP GET al servicio de cuentas para recuperar
   * todas las cuentas del cliente identificado por el parámetro customerId.
   * Maneja errores HTTP 4xx como ResourceNotFoundException y errores 5xx
   * como ResponseStatusException con estado SERVICE_UNAVAILABLE.
   * </p>
   *
   * @param customerId el identificador único del cliente
   * @return Flux de AccountResponse que emite todas las cuentas del cliente
   * @throws ResourceNotFoundException si no se encuentran cuentas para el cliente
   * @throws ResponseStatusException si el servicio de cuentas no está disponible
   *
   * @see AccountResponse
   * @see ResourceNotFoundException
   * @see ResponseStatusException
   */
  public Flux<AccountResponse> getAccountsByCustomer(String customerId) {
    log.info("Getting accounts for customer: {}", customerId);

    return webClient.get()
      .uri(accountServiceUrl + "/accounts/customer/{customerId}", customerId)
      .retrieve()
      .onStatus(status -> status.is4xxClientError(), response ->
        Mono.error(
          new ResourceNotFoundException("No accounts found for customer id: " + customerId))
      )
      .onStatus(status -> status.is5xxServerError(), response ->
        Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Account service unavailable"))
      )
      .bodyToFlux(AccountResponse.class)
      .doOnNext(account -> log.debug("Retrieved account: {} - {}", account.getAccountNumber(),
        account.getAccountType()))
      .doOnError(error -> log.error("Error getting accounts for customer {}: {}", customerId,
        error.getMessage()));
  }

  /**
   * Obtiene los detalles de una cuenta específica por su identificador.
   * <p>
   * Realiza una llamada HTTP GET al servicio de cuentas para recuperar
   * la información completa de una cuenta específica.
   * </p>
   *
   * @param accountId el identificador único de la cuenta
   * @return Mono de AccountResponse que contiene los detalles de la cuenta
   * @throws ResourceNotFoundException si la cuenta no existe
   * @throws ResponseStatusException si el servicio de cuentas no está disponible
   *
   * @see AccountResponse
   */
  public Mono<AccountResponse> getAccountById(String accountId) {
    log.info("Getting account by ID: {}", accountId);

    return webClient.get()
      .uri(accountServiceUrl + "/accounts/{accountId}", accountId)
      .retrieve()
      .onStatus(status -> status.is4xxClientError(), response ->
        Mono.error(new ResourceNotFoundException("Account not found with id: " + accountId))
      )
      .onStatus(status -> status.is5xxServerError(), response ->
        Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Account service unavailable"))
      )
      .bodyToMono(AccountResponse.class)
      .doOnSuccess(account -> log.debug("Retrieved account: {} - {}", account.getAccountNumber(),
        account.getAccountType()))
      .doOnError(
        error -> log.error("Error getting account by ID {}: {}", accountId, error.getMessage()));
  }

  /**
   * Obtiene el saldo actual de una cuenta específica.
   * <p>
   * Realiza una llamada HTTP GET al endpoint específico de saldo del servicio de cuentas.
   * Este método está optimizado para recuperar únicamente la información de saldo.
   * </p>
   *
   * @param accountId el identificador único de la cuenta
   * @return Mono de AccountResponse que contiene el saldo de la cuenta
   * @throws ResourceNotFoundException si la cuenta no existe
   * @throws ResponseStatusException si el servicio de cuentas no está disponible
   *
   * @see AccountResponse
   */
  public Mono<AccountResponse> getAccountBalance(String accountId) {
    log.info("Getting balance for account: {}", accountId);

    return webClient.get()
      .uri(accountServiceUrl + "/accounts/{accountId}/balance", accountId)
      .retrieve()
      .onStatus(status -> status.is4xxClientError(), response ->
        Mono.error(new ResourceNotFoundException("Account not found with id: " + accountId))
      )
      .onStatus(status -> status.is5xxServerError(), response ->
        Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Account service unavailable"))
      )
      .bodyToMono(AccountResponse.class)
      .doOnSuccess(balance -> log.debug("Retrieved balance for account {}: {}", accountId,
        balance.getBalance()))
      .doOnError(error -> log.error("Error getting balance for account {}: {}", accountId,
        error.getMessage()));
  }

  /**
   * Obtiene solo las cuentas activas de un cliente específico.
   * <p>
   * Filtra las cuentas del cliente para retornar únicamente aquellas
   * que tienen estado "ACTIVO". Utiliza el método getAccountsByCustomer
   * internamente y aplica el filtro sobre el resultado.
   * </p>
   *
   * @param customerId el identificador único del cliente
   * @return Flux de AccountResponse que emite solo las cuentas activas del cliente
   *
   * @see #getAccountsByCustomer(String)
   * @see AccountResponse
   */
  public Flux<AccountResponse> getActiveAccountsByCustomer(String customerId) {
    return getAccountsByCustomer(customerId)
      .filter(account -> "ACTIVO".equals(account.getStatus()))
      .doOnNext(account -> log.debug("Active account found: {} - {}", account.getAccountNumber(),
        account.getAccountType()));
  }

  /**
   * Valida si una cuenta existe y está en estado activo.
   * <p>
   * Realiza una verificación de existencia y estado de una cuenta.
   * Retorna true si la cuenta existe y tiene estado "ACTIVO",
   * false en cualquier otro caso (cuenta no existe o no está activa).
   * </p>
   *
   * @param accountId el identificador único de la cuenta a validar
   * @return Mono de Boolean que indica si la cuenta existe y está activa (true) o no (false)
   *
   * @see #getAccountById(String)
   */
  public Mono<Boolean> validateAccountExistsAndActive(String accountId) {
    return getAccountById(accountId)
      .map(account -> "ACTIVO".equals(account.getStatus()))
      .onErrorReturn(false)
      .doOnSuccess(exists -> log.debug("Account {} exists and active: {}", accountId, exists));
  }
}
