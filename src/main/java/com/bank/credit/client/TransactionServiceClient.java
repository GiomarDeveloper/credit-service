package com.bank.credit.client;

import com.bank.credit.model.response.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Cliente para consumir servicios del microservicio de transacciones.
 * <p>
 * Esta clase proporciona métodos para interactuar con el servicio de transacciones
 * mediante WebClient reactivo, permitiendo la recuperación de transacciones
 * asociadas a productos financieros para el mes actual.
 * </p>
 *
 * @see WebClient
 * @see Flux
 * @see Component
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

  private final WebClient webClient;

  @Value("${external.services.transaction.url:http://localhost:8084}")
  private String transactionServiceUrl;

  /**
   * Obtiene las transacciones de un producto específico para el mes actual.
   * <p>
   * Realiza una llamada HTTP GET al servicio de transacciones para recuperar
   * todas las transacciones asociadas a un producto financiero (cuenta, crédito, etc.)
   * que hayan ocurrido durante el mes actual. En caso de error, retorna un Flux vacío.
   * </p>
   *
   * @param productId el identificador único del producto financiero
   * @param productType el tipo de producto (ej: "ACCOUNT", "CREDIT", "LOAN")
   * @return Flux de Transaction que emite las transacciones del producto para el mes actual.
   *         Retorna Flux vacío en caso de error.
   *
   * @see Transaction
   * @see ParameterizedTypeReference
   */
  public Flux<Transaction> getProductTransactionsForCurrentMonth(String productId,
                                                                 String productType) {
    log.info("Getting transactions for product: {} of type: {} in current month", productId,
      productType);

    return webClient.get()
      .uri(transactionServiceUrl +
          "/transactions/product/{productId}/type/{productType}/current-month",
        productId, productType)
      .retrieve()
      .bodyToFlux(new ParameterizedTypeReference<Transaction>() {
      })
      .doOnError(ex -> {
        log.error("Error fetching transactions for product {} (type: {}): {}",
          productId, productType, ex.getMessage());
      })
      .onErrorResume(ex -> Flux.empty());
  }
}