package com.bank.credit.client;

import com.bank.credit.dto.CustomerResponse;
import com.bank.credit.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerServiceClient {

    private final WebClient webClient;

    @Value("${external.services.customer.url:http://localhost:8081}")
    private String customerServiceUrl;

    /**
     * Obtiene el tipo de cliente desde el customer-service
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
                .doOnError(error -> log.error("Error getting customer type for {}: {}", customerId, error.getMessage()));
    }
}