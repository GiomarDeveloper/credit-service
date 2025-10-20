package com.bank.credit.repository;

import com.bank.credit.model.Credit;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {
    Flux<Credit> findByCustomerId(String customerId);
    Flux<Credit> findByCreditType(String creditType);
    Flux<Credit> findByCustomerIdAndCreditType(String customerId, String creditType);
    Flux<Credit> findByStatus(String status);
}