package com.bank.credit.client;

import com.bank.credit.model.response.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final WebClient webClient;

    @Value("${external.services.transaction.url:http://localhost:8084}")
    private String transactionServiceUrl;

    public Flux<Transaction> getProductTransactionsForCurrentMonth(String productId, String productType) {
        log.info("Getting transactions for product: {} of type: {} in current month", productId, productType);

        return webClient.get()
                .uri(transactionServiceUrl + "/transactions/product/{productId}/type/{productType}/current-month",
                        productId, productType)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Transaction>() {})
                .doOnError(ex -> {
                    log.error("Error fetching transactions for product {} (type: {}): {}",
                            productId, productType, ex.getMessage());
                })
                .onErrorResume(ex -> Flux.empty());
    }
}