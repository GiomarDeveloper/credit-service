package com.bank.credit.controller;

import com.bank.credit.api.CreditsApi;
import com.bank.credit.model.*;
import com.bank.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class CreditController implements CreditsApi {

    private final CreditService creditService;

    @Override
    public Mono<ResponseEntity<CreditResponse>> chargeConsumption(String id, Mono<ConsumptionRequest> consumptionRequest, ServerWebExchange exchange) {
        return consumptionRequest
                .flatMap(request -> creditService.chargeConsumption(id, request))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<CreditResponse>> create(Mono<CreditRequest> creditRequest, ServerWebExchange exchange) {
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
    public Mono<ResponseEntity<Flux<CreditResponse>>> getAll(String customerId, String creditType, ServerWebExchange exchange) {
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
    public Mono<ResponseEntity<CreditBalanceResponse>> getCreditBalance(String id, ServerWebExchange exchange) {
        return creditService.getCreditBalance(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<CreditResponse>>> getCreditsByCustomer(String customerId, CreditTypeEnum creditType, ServerWebExchange exchange) {
        Flux<CreditResponse> credits = creditService.getCreditsByCustomer(customerId, creditType);
        return Mono.just(ResponseEntity.ok(credits));
    }

    @Override
    public Mono<ResponseEntity<CreditResponse>> makePayment(String id, Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest
                .flatMap(request -> creditService.makePayment(id, request))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<CreditResponse>> update(String id, Mono<CreditRequest> creditRequest, ServerWebExchange exchange) {
        return creditRequest
                .flatMap(request -> creditService.updateCredit(id, request))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<CreditDailyBalance>>> getCustomerCreditsWithDailyBalances(String customerId, ServerWebExchange exchange) {
        Flux<CreditDailyBalance> credits = creditService
                .getCustomerCreditsWithDailyBalances(customerId);

        return Mono.just(ResponseEntity.ok(credits));
    }
}