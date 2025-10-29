package com.bank.credit.controller;

import com.bank.credit.api.CreditsApi;
import com.bank.credit.exception.ResourceNotFoundException;
import com.bank.credit.model.ConsumptionRequest;
import com.bank.credit.model.CreditBalanceResponse;
import com.bank.credit.model.CreditDailyBalance;
import com.bank.credit.model.CreditRequest;
import com.bank.credit.model.CreditResponse;
import com.bank.credit.model.CreditTypeEnum;
import com.bank.credit.model.DebitCardMainAccountBalanceResponse;
import com.bank.credit.model.PaymentRequest;
import com.bank.credit.model.ThirdPartyPaymentRequest;
import com.bank.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controlador REST para la gestión de operaciones de créditos.
 * <p>
 * Este controlador implementa la interfaz CreditsApi generada por OpenAPI
 * y proporciona endpoints para todas las operaciones relacionadas con créditos,
 * incluyendo creación, consulta, pagos, consumos y eliminación de créditos.
 * </p>
 * <p>
 * Todas las operaciones son reactivas y devuelven tipos Mono y Flux para
 * soportar programación no bloqueante.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class CreditController implements CreditsApi {

  private final CreditService creditService;

  @Override
  public Mono<ResponseEntity<CreditResponse>> chargeConsumption(String id,
                                                                Mono<ConsumptionRequest> consumptionRequest,
                                                                ServerWebExchange exchange) {
    return consumptionRequest
      .flatMap(request -> creditService.chargeConsumption(id, request))
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @Override
  public Mono<ResponseEntity<CreditResponse>> create(Mono<CreditRequest> creditRequest,
                                                     ServerWebExchange exchange) {
    return creditRequest
      .flatMap(creditService::createCredit)
      .map(credit -> ResponseEntity.status(HttpStatus.CREATED).body(credit))
      .defaultIfEmpty(ResponseEntity.badRequest().build());
  }

  @Override
  public Mono<ResponseEntity<Void>> delete(String id, ServerWebExchange exchange) {
    return creditService.deleteCredit(id)
      .then(Mono.just(ResponseEntity.noContent().build()));
  }

  @Override
  public Mono<ResponseEntity<Flux<CreditResponse>>> getAll(String customerId, String creditType,
                                                           ServerWebExchange exchange) {
    Flux<CreditResponse> credits = creditService.getAllCredits(customerId, creditType);
    return Mono.just(ResponseEntity.ok(credits));
  }

  @Override
  public Mono<ResponseEntity<CreditResponse>> getById(String id, ServerWebExchange exchange) {
    return creditService.getCreditById(id)
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @Override
  public Mono<ResponseEntity<CreditBalanceResponse>> getCreditBalance(String id,
                                                                      ServerWebExchange exchange) {
    return creditService.getCreditBalance(id)
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @Override
  public Mono<ResponseEntity<Flux<CreditResponse>>> getCreditsByCustomer(String customerId,
                                                                         CreditTypeEnum creditType,
                                                                         ServerWebExchange exchange) {
    Flux<CreditResponse> credits = creditService.getCreditsByCustomer(customerId, creditType);
    return Mono.just(ResponseEntity.ok(credits));
  }

  @Override
  public Mono<ResponseEntity<CreditResponse>> makePayment(String id,
                                                          Mono<PaymentRequest> paymentRequest,
                                                          ServerWebExchange exchange) {
    return paymentRequest
      .flatMap(request -> creditService.makePayment(id, request))
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @Override
  public Mono<ResponseEntity<CreditResponse>> update(String id, Mono<CreditRequest> creditRequest,
                                                     ServerWebExchange exchange) {
    return creditRequest
      .flatMap(request -> creditService.updateCredit(id, request))
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @Override
  public Mono<ResponseEntity<Flux<CreditDailyBalance>>> getCustomerCreditsWithDailyBalances(
    String customerId, ServerWebExchange exchange) {
    Flux<CreditDailyBalance> credits = creditService
      .getCustomerCreditsWithDailyBalances(customerId);

    return Mono.just(ResponseEntity.ok(credits));
  }

  @Override
  public Mono<ResponseEntity<Boolean>> hasOverdueCredits(String customerId,
                                                         ServerWebExchange exchange) {
    return creditService.hasOverdueCredits(customerId)
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.ok(false));
  }

  @Override
  public Mono<ResponseEntity<CreditResponse>> makeThirdPartyPayment(String creditId,
                                                                    Mono<ThirdPartyPaymentRequest> thirdPartyPaymentRequest,
                                                                    ServerWebExchange exchange) {
    return thirdPartyPaymentRequest
      .flatMap(request -> creditService.makeThirdPartyPayment(creditId, request))
      .map(ResponseEntity::ok)
      .onErrorResume(ResourceNotFoundException.class, ex -> {
        return Mono.just(ResponseEntity.notFound().build());
      })
      .onErrorResume(IllegalArgumentException.class, ex -> {
        return Mono.just(ResponseEntity.badRequest().build());
      })
      .onErrorResume(ex -> {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
      });

  }


  @Override
  public Mono<ResponseEntity<DebitCardMainAccountBalanceResponse>> getDebitCardMainAccountBalance(
    String cardId, ServerWebExchange exchange) {
    return creditService.getDebitCardMainAccountBalance(cardId)
      .map(ResponseEntity::ok)
      .onErrorResume(ResourceNotFoundException.class, ex ->
        Mono.just(ResponseEntity.notFound().build()))
      .onErrorResume(IllegalArgumentException.class, ex ->
        Mono.just(ResponseEntity.badRequest().build()))
      .onErrorResume(ex ->
        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }
}