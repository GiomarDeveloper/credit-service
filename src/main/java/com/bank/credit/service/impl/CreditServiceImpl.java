package com.bank.credit.service.impl;

import com.bank.credit.client.CustomerServiceClient;
import com.bank.credit.exception.ResourceNotFoundException;
import com.bank.credit.mapper.CreditMapper;
import com.bank.credit.model.*;
import com.bank.credit.repository.CreditRepository;
import com.bank.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditRepository creditRepository;
    private final CreditMapper creditMapper;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public Flux<CreditResponse> getAllCredits(String customerId, String creditType) {
        log.info("Getting credits - customerId: {}, creditType: {}", customerId, creditType);

        if (customerId != null && creditType != null) {
            return creditRepository.findByCustomerIdAndCreditType(customerId, creditType)
                    .map(creditMapper::toResponse);
        } else if (customerId != null) {
            return creditRepository.findByCustomerId(customerId)
                    .map(creditMapper::toResponse);
        } else if (creditType != null) {
            return creditRepository.findByCreditType(creditType)
                    .map(creditMapper::toResponse);
        } else {
            return creditRepository.findAll()
                    .map(creditMapper::toResponse);
        }
    }

    @Override
    public Mono<CreditResponse> getCreditById(String id) {
        log.info("Getting credit by id: {}", id);

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + id)))
                .map(creditMapper::toResponse)
                .doOnSuccess(credit -> log.debug("Credit found: {}", credit.getId()))
                .doOnError(error -> log.warn("Credit not found: {}", id));
    }

    @Override
    public Mono<CreditResponse> createCredit(CreditRequest creditRequest) {
        log.info("Creating credit for customer: {}, type: {}",
                creditRequest.getCustomerId(), creditRequest.getCreditType());

        return customerServiceClient.getCustomerType(creditRequest.getCustomerId())
                .flatMap(customerType -> validateBusinessRules(creditRequest, customerType))
                .flatMap(validatedRequest -> {
                    Credit credit = creditMapper.toEntity(validatedRequest);
                    credit.setCreatedAt(Instant.now());
                    credit.setUpdatedAt(Instant.now());
                    credit.setStatus("ACTIVO");

                    // Inicializar balances según el tipo de crédito
                    if ("TARJETA_CREDITO".equals(credit.getCreditType())) {
                        credit.setOutstandingBalance(0.0);
                        credit.setAvailableCredit(credit.getCreditLimit());
                    } else {
                        credit.setOutstandingBalance(credit.getAmount());
                        credit.setAvailableCredit(0.0);
                        credit.setRemainingPayments(credit.getTermMonths());
                    }

                    return creditRepository.save(credit)
                            .map(creditMapper::toResponse)
                            .doOnSuccess(created -> log.info("Credit created successfully: {}", created.getId()))
                            .doOnError(error -> log.error("Error creating credit: {}", error.getMessage()));
                })
                .onErrorResume(ex -> {
                    log.error("Error creating credit for customer {}: {}",
                            creditRequest.getCustomerId(), ex.getMessage());
                    return Mono.error(ex);
                });
    }

    @Override
    public Mono<CreditResponse> updateCredit(String id, CreditRequest creditRequest) {
        log.info("Updating credit: {}", id);

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + id)))
                .flatMap(existingCredit -> {
                    Credit updatedCredit = creditMapper.toEntity(creditRequest);
                    updatedCredit.setId(existingCredit.getId());
                    updatedCredit.setCreatedAt(existingCredit.getCreatedAt());
                    updatedCredit.setUpdatedAt(Instant.now());
                    // Preservar campos que no deben cambiar en update
                    updatedCredit.setOutstandingBalance(existingCredit.getOutstandingBalance());
                    updatedCredit.setAvailableCredit(existingCredit.getAvailableCredit());
                    updatedCredit.setRemainingPayments(existingCredit.getRemainingPayments());
                    updatedCredit.setStatus(existingCredit.getStatus());

                    return creditRepository.save(updatedCredit)
                            .map(creditMapper::toResponse)
                            .doOnSuccess(updated -> log.info("Credit updated successfully: {}", updated.getId()));
                });
    }

    @Override
    public Mono<Void> deleteCredit(String id) {
        log.info("Deleting credit: {}", id);

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + id)))
                .flatMap(credit -> creditRepository.deleteById(id))
                .doOnSuccess(v -> log.info("Credit deleted successfully: {}", id))
                .doOnError(error -> log.warn("Error deleting credit {}: {}", id, error.getMessage()));
    }

    @Override
    public Flux<CreditResponse> getCreditsByCustomer(String customerId) {
        log.info("Getting credits for customer: {}", customerId);

        return creditRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No credits found for customer: " + customerId)))
                .map(creditMapper::toResponse)
                .doOnComplete(() -> log.debug("Retrieved credits for customer: {}", customerId));
    }

    @Override
    public Mono<CreditBalanceResponse> getCreditBalance(String id) {
        log.info("Getting balance for credit: {}", id);

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + id)))
                .map(creditMapper::toBalanceResponse)
                .doOnSuccess(balance -> log.debug("Balance retrieved for credit: {}", id));
    }

    @Override
    public Mono<CreditResponse> makePayment(String id, PaymentRequest paymentRequest) {
        log.info("Processing payment for credit: {}, amount: {}", id, paymentRequest.getAmount());

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + id)))
                .flatMap(credit -> {
                    // Validar que el crédito esté activo
                    if (!"ACTIVO".equals(credit.getStatus())) {
                        return Mono.error(new IllegalArgumentException("Cannot make payment to inactive or paid credit"));
                    }

                    if (paymentRequest.getAmount() > credit.getOutstandingBalance()) {
                        return Mono.error(new IllegalArgumentException("Payment amount exceeds outstanding balance"));
                    }

                    // Procesar pago
                    credit.setOutstandingBalance(credit.getOutstandingBalance() - paymentRequest.getAmount());

                    if ("TARJETA_CREDITO".equals(credit.getCreditType())) {
                        credit.setAvailableCredit(credit.getAvailableCredit() + paymentRequest.getAmount());
                    } else {
                        credit.setRemainingPayments(credit.getRemainingPayments() - 1);
                    }

                    // Verificar si el crédito queda pagado
                    if (credit.getOutstandingBalance() <= 0) {
                        credit.setStatus("PAGADO");
                        log.info("Credit fully paid: {}", id);
                    }

                    credit.setUpdatedAt(Instant.now());

                    return creditRepository.save(credit)
                            .map(creditMapper::toResponse)
                            .doOnSuccess(updated -> log.info("Payment processed successfully for credit: {}", id));
                });
    }

    @Override
    public Mono<CreditResponse> chargeConsumption(String id, ConsumptionRequest consumptionRequest) {
        log.info("Processing consumption for credit: {}, amount: {}, merchant: {}",
                id, consumptionRequest.getAmount(), consumptionRequest.getMerchant());

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + id)))
                .flatMap(credit -> {
                    // Validar que sea tarjeta de crédito
                    if (!"TARJETA_CREDITO".equals(credit.getCreditType())) {
                        return Mono.error(new IllegalArgumentException("Consumption can only be charged to credit cards"));
                    }

                    // Validar que esté activa
                    if (!"ACTIVO".equals(credit.getStatus())) {
                        return Mono.error(new IllegalArgumentException("Cannot charge consumption to inactive credit card"));
                    }

                    if (consumptionRequest.getAmount() > credit.getAvailableCredit()) {
                        return Mono.error(new IllegalArgumentException("Consumption amount exceeds available credit"));
                    }

                    // Procesar consumo
                    credit.setOutstandingBalance(credit.getOutstandingBalance() + consumptionRequest.getAmount());
                    credit.setAvailableCredit(credit.getAvailableCredit() - consumptionRequest.getAmount());
                    credit.setUpdatedAt(Instant.now());

                    return creditRepository.save(credit)
                            .map(creditMapper::toResponse)
                            .doOnSuccess(updated -> log.info("Consumption charged successfully for credit: {}", id));
                });
    }

    private Mono<CreditRequest> validateBusinessRules(CreditRequest creditRequest, String customerType) {
        log.debug("Validating business rules for customer: {}, type: {}",
                creditRequest.getCustomerId(), customerType);

        // 1. Validar que clientes PERSONALES no puedan tener PRESTAMO_EMPRESARIAL
        if ("PERSONAL".equals(customerType) && "PRESTAMO_EMPRESARIAL".equals(creditRequest.getCreditType().getValue())) {
            return Mono.error(new IllegalArgumentException("Personal customers cannot have business loans"));
        }

        // 2. Validar límite de 1 préstamo personal por cliente personal
        if ("PERSONAL".equals(customerType) && "PRESTAMO_PERSONAL".equals(creditRequest.getCreditType().getValue())) {
            return creditRepository.findByCustomerIdAndCreditType(creditRequest.getCustomerId(), "PRESTAMO_PERSONAL")
                    .hasElements()
                    .flatMap(hasExistingPersonalLoan -> {
                        if (hasExistingPersonalLoan) {
                            return Mono.error(new IllegalArgumentException("Personal customers can only have one personal loan"));
                        }
                        return Mono.just(creditRequest);
                    });
        }

        // 3. Validar que tarjetas de crédito tengan límite de crédito
        if ("TARJETA_CREDITO".equals(creditRequest.getCreditType().getValue()) &&
                (creditRequest.getCreditLimit() == null || creditRequest.getCreditLimit() <= 0)) {
            return Mono.error(new IllegalArgumentException("Credit cards must have a credit limit greater than 0"));
        }

        // 4. Validar que préstamos tengan plazo en meses
        if ((creditRequest.getCreditType().getValue().equals("PRESTAMO_PERSONAL") ||
                creditRequest.getCreditType().getValue().equals("PRESTAMO_EMPRESARIAL")) &&
                (creditRequest.getTermMonths() == null || creditRequest.getTermMonths() <= 0)) {
            return Mono.error(new IllegalArgumentException("Loans must have a term in months greater than 0"));
        }

        // 5. Validar que préstamos no tengan límite de crédito
        if ((creditRequest.getCreditType().getValue().equals("PRESTAMO_PERSONAL") ||
                creditRequest.getCreditType().getValue().equals("PRESTAMO_EMPRESARIAL")) &&
                creditRequest.getCreditLimit() != null && creditRequest.getCreditLimit() > 0) {
            return Mono.error(new IllegalArgumentException("Loans cannot have credit limit"));
        }

        // 6. Validar que tarjetas no tengan plazo ni pago mensual
        if ("TARJETA_CREDITO".equals(creditRequest.getCreditType().getValue())) {
            if (creditRequest.getTermMonths() != null && creditRequest.getTermMonths() > 0) {
                return Mono.error(new IllegalArgumentException("Credit cards cannot have term months"));
            }
            if (creditRequest.getMonthlyPayment() != null && creditRequest.getMonthlyPayment() > 0) {
                return Mono.error(new IllegalArgumentException("Credit cards cannot have monthly payment"));
            }
        }

        // 7. Validar montos positivos
        if ("TARJETA_CREDITO".equals(creditRequest.getCreditType().getValue())) {
            if (creditRequest.getTermMonths() != null && creditRequest.getTermMonths() > 0) {
                return Mono.error(new IllegalArgumentException("Credit cards cannot have term months"));
            }
            if (creditRequest.getMonthlyPayment() != null && creditRequest.getMonthlyPayment() > 0) {
                return Mono.error(new IllegalArgumentException("Credit cards cannot have monthly payment"));
            }
        }

        log.debug("Business rules validation passed for customer: {}", creditRequest.getCustomerId());
        return Mono.just(creditRequest);
    }
}