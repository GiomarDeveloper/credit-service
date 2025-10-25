package com.bank.credit.service.impl;

import com.bank.credit.client.CustomerServiceClient;
import com.bank.credit.client.TransactionServiceClient;
import com.bank.credit.exception.ResourceNotFoundException;
import com.bank.credit.mapper.CreditMapper;
import com.bank.credit.model.*;
import com.bank.credit.model.response.Transaction;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditRepository creditRepository;
    private final CreditMapper creditMapper;
    private final CustomerServiceClient customerServiceClient;
    private final TransactionServiceClient transactionServiceClient;

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
    public Flux<CreditResponse> getCreditsByCustomer(String customerId, CreditTypeEnum creditType) {
        log.info("Getting credits for customer: {} with type: {}", customerId, creditType);

        if (creditType != null) {
            // Filtrar por customerId y creditType
            return creditRepository.findByCustomerIdAndCreditType(customerId, creditType.toString())
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                            "No credits found for customer: " + customerId + " with type: " + creditType)))
                    .map(creditMapper::toResponse)
                    .doOnComplete(() -> log.debug("Retrieved credits for customer: {} with type: {}", customerId, creditType));
        } else {
            // Solo filtrar por customerId
            return creditRepository.findByCustomerId(customerId)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("No credits found for customer: " + customerId)))
                    .map(creditMapper::toResponse)
                    .doOnComplete(() -> log.debug("Retrieved all credits for customer: {}", customerId));
        }
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

    @Override
    public Flux<CreditDailyBalance> getCustomerCreditsWithDailyBalances(String customerId) {
        log.info("Getting credits with daily balances for customer: {}", customerId);

        return creditRepository.findByCustomerId(customerId)
                .flatMap(credit ->
                        calculateRealDailyBalances(credit)
                                .map(dailyBalances -> createCreditDailyBalance(credit, dailyBalances))
                )
                .onErrorResume(ex -> {
                    log.error("Error calculating daily balances for customer {}: {}", customerId, ex.getMessage());
                    return Flux.empty();
                });
    }

    private Mono<List<DailyBalance>> calculateRealDailyBalances(Credit credit) {
        return transactionServiceClient.getProductTransactionsForCurrentMonth(credit.getId(), "CREDITO")
                .collectList()
                .map(transactions -> calculateDailyBalancesFromTransactions(credit, transactions));
    }

    private List<DailyBalance> calculateDailyBalancesFromTransactions(Credit credit, List<Transaction> transactions) {
        // Obtener fecha de creación del crédito USANDO ZONA HORARIA DEL SERVIDOR
        LocalDate creditCreationDate = credit.getCreatedAt() != null ?
                credit.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate() :
                LocalDate.now();

        // Agrupar transacciones por día
        Map<String, List<Transaction>> transactionsByDay = transactions.stream()
                .filter(transaction -> transaction.getTransactionDate() != null)
                .collect(Collectors.groupingBy(transaction -> {
                    String dateStr = transaction.getTransactionDate();
                    if (dateStr.length() >= 10) {
                        return dateStr.substring(0, 10); // YYYY-MM-DD
                    }
                    return "unknown";
                }));

        List<DailyBalance> dailyBalances = new ArrayList<>();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        // Empezar desde el balance actual
        double runningBalance = -credit.getOutstandingBalance();

        for (LocalDate date = today; !date.isBefore(startOfMonth); date = date.minusDays(1)) {
            String dateStr = date.toString();
            List<Transaction> dayTransactions = transactionsByDay.getOrDefault(dateStr, new ArrayList<>());

            // Si el crédito no existía este día, saldo = 0
            if (date.isBefore(creditCreationDate)) {
                DailyBalance dailyBalance = new DailyBalance();
                dailyBalance.setDate(date);
                dailyBalance.setBalance(0.0);
                dailyBalance.setTransactionsCount(0);
                dailyBalances.add(0, dailyBalance);
                continue;
            }

            // Revertir transacciones para calcular saldo inicial del día
            double dayStartBalance = runningBalance;
            for (Transaction tx : dayTransactions) {
                if ("PAGO_CREDITO".equals(tx.getTransactionType())) {
                    // PAGO: Revertir el pago (aumentar la deuda)
                    dayStartBalance += tx.getAmount(); // +2000 hace la deuda más negativa
                } else if ("CONSUMO_TARJETA".equals(tx.getTransactionType())) {
                    // CONSUMO: Revertir el consumo (disminuir la deuda)
                    dayStartBalance -= tx.getAmount(); // -amount hace la deuda menos negativa
                }
            }

            DailyBalance dailyBalance = new DailyBalance();
            dailyBalance.setDate(date);
            dailyBalance.setBalance(dayStartBalance);
            dailyBalance.setTransactionsCount(dayTransactions.size());

            dailyBalances.add(0, dailyBalance);
            runningBalance = dayStartBalance;
        }

        return dailyBalances;
    }

    private CreditDailyBalance createCreditDailyBalance(Credit credit, List<DailyBalance> dailyBalances) {
        double dailyAverage = calculateDailyAverage(dailyBalances);

        CreditDailyBalance response = new CreditDailyBalance();
        response.setId(credit.getId());
        response.setCreditNumber(credit.getCreditNumber());
        response.setCreditType(CreditTypeEnum.valueOf(credit.getCreditType()));
        response.setCustomerId(credit.getCustomerId());
        response.setCurrentBalance(-credit.getOutstandingBalance());
        response.setDailyAverage(dailyAverage);
        response.setDailyBalances(dailyBalances);
        return response;
    }

    private double calculateDailyAverage(List<DailyBalance> dailyBalances) {
        if (dailyBalances == null || dailyBalances.isEmpty()) {
            return 0.0;
        }

        double sum = dailyBalances.stream()
                .mapToDouble(DailyBalance::getBalance)
                .sum();

        return sum / dailyBalances.size();
    }

    private Mono<CreditRequest> validateBusinessRules(CreditRequest creditRequest, String customerType) {
        log.debug("Validating business rules for customer: {}, type: {}",
                creditRequest.getCustomerId(), customerType);

        // 1. Validar que clientes PERSONALES no puedan tener PRESTAMO_EMPRESARIAL
        if (("PERSONAL".equals(customerType) && "PRESTAMO_EMPRESARIAL".equals(creditRequest.getCreditType().getValue()))
        || ("PERSONAL_VIP".equals(customerType) && "PRESTAMO_EMPRESARIAL".equals(creditRequest.getCreditType().getValue()))
        ) {
            return Mono.error(new IllegalArgumentException("Personal customers cannot have business loans"));
        }

        // 2. Validar límite de 1 préstamo personal por cliente personal
        if (("PERSONAL".equals(customerType) && "PRESTAMO_PERSONAL".equals(creditRequest.getCreditType().getValue()))
        || ("PERSONAL_VIP".equals(customerType) && "PRESTAMO_PERSONAL".equals(creditRequest.getCreditType().getValue()))
        ) {
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