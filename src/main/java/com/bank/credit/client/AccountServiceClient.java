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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final WebClient webClient;

    @Value("${external.services.account.url:http://localhost:8082}")
    private String accountServiceUrl;

    /**
     * Obtiene todas las cuentas de un cliente
     */
    public Flux<AccountResponse> getAccountsByCustomer(String customerId) {
        log.info("Getting accounts for customer: {}", customerId);

        return webClient.get()
                .uri(accountServiceUrl + "/accounts/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        Mono.error(new ResourceNotFoundException("No accounts found for customer id: " + customerId))
                )
                .onStatus(status -> status.is5xxServerError(), response ->
                        Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Account service unavailable"))
                )
                .bodyToFlux(AccountResponse.class)
                .doOnNext(account -> log.debug("Retrieved account: {} - {}", account.getAccountNumber(), account.getAccountType()))
                .doOnError(error -> log.error("Error getting accounts for customer {}: {}", customerId, error.getMessage()));
    }

    /**
     * Obtiene una cuenta específica por ID
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
                .doOnSuccess(account -> log.debug("Retrieved account: {} - {}", account.getAccountNumber(), account.getAccountType()))
                .doOnError(error -> log.error("Error getting account by ID {}: {}", accountId, error.getMessage()));
    }

    /**
     * Obtiene el saldo de una cuenta específica
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
                .doOnSuccess(balance -> log.debug("Retrieved balance for account {}: {}", accountId, balance.getBalance()))
                .doOnError(error -> log.error("Error getting balance for account {}: {}", accountId, error.getMessage()));
    }

    /**
     * Obtiene solo las cuentas activas de un cliente
     */
    public Flux<AccountResponse> getActiveAccountsByCustomer(String customerId) {
        return getAccountsByCustomer(customerId)
                .filter(account -> "ACTIVO".equals(account.getStatus()))
                .doOnNext(account -> log.debug("Active account found: {} - {}", account.getAccountNumber(), account.getAccountType()));
    }

    /**
     * Verifica si una cuenta existe y está activa
     */
    public Mono<Boolean> validateAccountExistsAndActive(String accountId) {
        return getAccountById(accountId)
                .map(account -> "ACTIVO".equals(account.getStatus()))
                .onErrorReturn(false)
                .doOnSuccess(exists -> log.debug("Account {} exists and active: {}", accountId, exists));
    }
}
