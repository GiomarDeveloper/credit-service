package com.bank.credit.service.impl;

import com.bank.credit.client.AccountServiceClient;
import com.bank.credit.client.CustomerServiceClient;
import com.bank.credit.client.TransactionServiceClient;
import com.bank.credit.exception.ResourceNotFoundException;
import com.bank.credit.mapper.CreditMapper;
import com.bank.credit.model.*;
import com.bank.credit.model.response.AccountResponse;
import com.bank.credit.model.response.Transaction;
import com.bank.credit.repository.CreditRepository;
import com.bank.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditRepository creditRepository;
    private final CreditMapper creditMapper;
    private final CustomerServiceClient customerServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final AccountServiceClient accountServiceClient;

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
                .flatMap(validatedRequest -> hasOverdueCredits(validatedRequest.getCustomerId())
                        .flatMap(hasOverdue -> {
                            if (hasOverdue) {
                                log.warn("Customer {} has overdue credits, cannot create new credit product",
                                        validatedRequest.getCustomerId());
                                return Mono.error(new ResourceNotFoundException(
                                        "El cliente tiene deudas vencidas. No puede adquirir nuevos productos de crédito."));
                            }
                            return Mono.just(validatedRequest);
                        })
                )
                .flatMap(validatedRequest -> {
                    // VALIDACIONES ESPECÍFICAS PARA TARJETAS DE DÉBITO
                    if ("TARJETA_DEBITO".equals(validatedRequest.getCreditType().getValue())) {
                        return validateDebitCardAccounts(validatedRequest)
                                .flatMap(this::createCreditEntity);
                    } else {
                        return createCreditEntity(validatedRequest);
                    }
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

    @Override
    public Mono<Boolean> hasOverdueCredits(String customerId) {
        LocalDate today = LocalDate.now();

        return creditRepository.findByCustomerIdAndStatus(customerId, "ACTIVO")
                .any(credit -> {
                    // Verificar si la fecha de vencimiento es anterior a hoy
                    return credit.getDueDate() != null &&
                            credit.getDueDate().isBefore(today);
                });
    }

    @Override
    public Mono<CreditResponse> makeThirdPartyPayment(String creditId, ThirdPartyPaymentRequest request) {
        log.info("Processing third party payment for credit: {}, amount: {}, payer: {}",
                creditId, request.getAmount(), request.getPayerCustomerId());

        return creditRepository.findById(creditId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Credit not found with id: " + creditId)))
                .flatMap(credit -> validateThirdPartyPayment(credit, request))
                .flatMap(credit -> processThirdPartyPayment(credit, request))
                .flatMap(creditRepository::save)
                .map(creditMapper::toResponse)
                .doOnSuccess(updated -> log.info("Third party payment processed successfully for credit: {}", creditId))
                .doOnError(error -> log.error("Error processing third party payment: {}", error.getMessage()));
    }

    @Override
    public Mono<DebitCardMainAccountBalanceResponse> getDebitCardMainAccountBalance(String cardId) {
        log.info("Getting main account balance for debit card: {}", cardId);

        return creditRepository.findById(cardId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit card not found with id: " + cardId)))
                .flatMap(credit -> {
                    // Validar que sea tarjeta débito
                    if (!"TARJETA_DEBITO".equals(credit.getCreditType())) {
                        return Mono.error(new IllegalArgumentException(
                                "Product is not a debit card. Type: " + credit.getCreditType()));
                    }

                    // Validar que tenga cuenta principal
                    if (credit.getMainAccountId() == null) {
                        return Mono.error(new IllegalArgumentException("Debit card has no main account associated"));
                    }

                    // Obtener información de la cuenta principal
                    return accountServiceClient.getAccountById(credit.getMainAccountId())
                            .map(accountInfo -> createDebitCardBalanceResponse(credit, accountInfo));
                })
                .doOnSuccess(response -> log.info("Main account balance retrieved for debit card: {}", cardId))
                .doOnError(error -> log.error("Error retrieving main account balance: {}", error.getMessage()));
    }

    private DebitCardMainAccountBalanceResponse createDebitCardBalanceResponse(
            Credit credit, AccountResponse accountInfo) {

        DebitCardMainAccountBalanceResponse response = new DebitCardMainAccountBalanceResponse();
        response.setCardId(credit.getId());
        response.setCardNumber(credit.getCreditNumber());
        response.setCardStatus(credit.getCardStatus());
        response.setMainAccountId(credit.getMainAccountId());
        response.setAccountNumber(accountInfo.getAccountNumber());
        response.setAccountType(accountInfo.getAccountType());
        response.setCurrentBalance(accountInfo.getBalance());
        response.setAvailableBalance(accountInfo.getBalance());
        response.setCurrency("PEN");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.ZoneOffset serverOffset = java.time.ZoneId.systemDefault().getRules().getOffset(now);
        java.time.OffsetDateTime offsetDateTime = now.atOffset(serverOffset);
        response.setLastUpdated(offsetDateTime);

        return response;
    }

    private Mono<CreditRequest> validateDebitCardAccounts(CreditRequest creditRequest) {
        log.info("Validating debit card accounts for customer: {}", creditRequest.getCustomerId());

        // 1. Validar que tenga cuenta principal
        if (creditRequest.getMainAccountId() == null || creditRequest.getMainAccountId().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Las tarjetas de débito requieren una cuenta principal asociada"));
        }

        // 2. Validar límites diarios
        if (creditRequest.getDailyWithdrawalLimit() == null || creditRequest.getDailyWithdrawalLimit() <= 0) {
            return Mono.error(new IllegalArgumentException("Se requiere un límite diario de retiro válido mayor a 0"));
        }

        if (creditRequest.getDailyPurchaseLimit() == null || creditRequest.getDailyPurchaseLimit() <= 0) {
            return Mono.error(new IllegalArgumentException("Se requiere un límite diario de compra válido mayor a 0"));
        }

        // 3. Validar marca de tarjeta - CORREGIDO
        if (creditRequest.getCardBrand() == null) {
            return Mono.error(new IllegalArgumentException("Se requiere especificar la marca de la tarjeta (VISA o MASTERCARD)"));
        }

        // 4. Validar que la cuenta principal pertenezca al cliente
        return accountServiceClient.getAccountById(creditRequest.getMainAccountId())
                .flatMap(mainAccount -> {
                    // Verificar que la cuenta principal pertenezca al cliente
                    if (!creditRequest.getCustomerId().equals(mainAccount.getCustomerId())) {
                        return Mono.error(new IllegalArgumentException(
                                "La cuenta principal no pertenece al cliente. Cuenta: " + mainAccount.getId() +
                                        ", Cliente dueño: " + mainAccount.getCustomerId() +
                                        ", Cliente solicitante: " + creditRequest.getCustomerId()));
                    }

                    // Verificar que la cuenta principal esté activa
                    if (!"ACTIVO".equals(mainAccount.getStatus())) {
                        return Mono.error(new IllegalArgumentException(
                                "La cuenta principal no está activa. Estado: " + mainAccount.getStatus()));
                    }

                    // 5. Validar cuentas asociadas si existen - CORREGIDO
                    if (creditRequest.getAssociatedAccounts() != null && !creditRequest.getAssociatedAccounts().isEmpty()) {
                        return validateAssociatedAccounts(creditRequest, mainAccount);
                    }

                    return Mono.just(creditRequest);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "La cuenta principal no existe: " + creditRequest.getMainAccountId())));
    }


    /**
     * Validar que las cuentas asociadas pertenezcan al cliente
     */
    private Mono<CreditRequest> validateAssociatedAccounts(CreditRequest creditRequest, AccountResponse mainAccount) {
        log.info("Validating {} associated accounts for customer: {}",
                creditRequest.getAssociatedAccounts().size(), creditRequest.getCustomerId());

        return Flux.fromIterable(creditRequest.getAssociatedAccounts())
                .flatMap(associatedAccount -> accountServiceClient.getAccountById(associatedAccount.getAccountId())
                        .flatMap(account -> {
                            // Verificar que la cuenta asociada pertenezca al cliente
                            if (!creditRequest.getCustomerId().equals(account.getCustomerId())) {
                                return Mono.error(new IllegalArgumentException(
                                        "La cuenta asociada no pertenece al cliente. Cuenta: " + account.getId() +
                                                ", Cliente dueño: " + account.getCustomerId() +
                                                ", Cliente solicitante: " + creditRequest.getCustomerId()));
                            }

                            // Verificar que la cuenta asociada esté activa
                            if (!"ACTIVO".equals(account.getStatus())) {
                                return Mono.error(new IllegalArgumentException(
                                        "La cuenta asociada no está activa. Cuenta: " + account.getId() +
                                                ", Estado: " + account.getStatus()));
                            }

                            // Verificar que no sea la misma cuenta principal
                            if (account.getId().equals(creditRequest.getMainAccountId())) {
                                return Mono.error(new IllegalArgumentException(
                                        "La cuenta principal no puede estar en la lista de cuentas asociadas"));
                            }

                            // Validar que no haya sequenceOrder duplicados
                            validateSequenceOrder(creditRequest.getAssociatedAccounts());

                            return Mono.just(account);
                        })
                )
                .collectList()
                .thenReturn(creditRequest);
    }

    private void validateSequenceOrder(List<CreditRequestAssociatedAccountsInner> associatedAccounts) {
        Set<Integer> orders = new HashSet<>();
        for (CreditRequestAssociatedAccountsInner acc : associatedAccounts) {
            if (!orders.add(acc.getSequenceOrder())) {
                throw new IllegalArgumentException("sequenceOrder duplicado: " + acc.getSequenceOrder());
            }
        }
    }

    /**
     * Crear la entidad Credit (compartido para todos los tipos)
     */
    private Mono<CreditResponse> createCreditEntity(CreditRequest validatedRequest) {
        Credit credit = creditMapper.toEntity(validatedRequest);
        credit.setCreatedAt(Instant.now());
        credit.setUpdatedAt(Instant.now());
        credit.setStatus("ACTIVO");

        // Lógica específica por tipo de producto
        switch (validatedRequest.getCreditType().getValue()) {
            case "TARJETA_CREDITO":
                credit.setOutstandingBalance(0.0);
                credit.setAvailableCredit(credit.getCreditLimit());
                break;

            case "TARJETA_DEBITO":
                // Configuración específica para tarjetas de débito
                credit.setOutstandingBalance(0.0);
                credit.setAvailableCredit(0.0);
                credit.setAmount(0.0); // No aplica para débito
                credit.setInterestRate(0.0); // No aplica para débito
                credit.setExpirationDate(LocalDate.now().plusYears(3)); // 3 años de vigencia
                credit.setCardStatus("INACTIVA"); // Se activa posteriormente
                break;

            default: // PRÉSTAMOS
                credit.setOutstandingBalance(credit.getAmount());
                credit.setAvailableCredit(0.0);
                credit.setRemainingPayments(credit.getTermMonths());
                break;
        }

        return creditRepository.save(credit)
                .map(creditMapper::toResponse)
                .doOnSuccess(created -> {
                    log.info("Credit created successfully: {} - Type: {}",
                            created.getId(), created.getCreditType());

                    // Log específico para tarjetas de débito
                    if ("TARJETA_DEBITO".equals(created.getCreditType().getValue())) {
                        log.info("Debit card created - Main Account: {}, Daily Limits - Withdrawal: {}, Purchase: {}",
                                created.getMainAccountId(),
                                created.getDailyWithdrawalLimit(),
                                created.getDailyPurchaseLimit());
                    }
                })
                .doOnError(error -> log.error("Error creating credit: {}", error.getMessage()));
    }

    /**
     * Actualizar validateBusinessRules para incluir validaciones de débito
     */
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

        // 7. Validaciones específicas para tarjetas de débito
        if ("TARJETA_DEBITO".equals(creditRequest.getCreditType().getValue())) {
            // Para débito, no deben tener campos de crédito
            if (creditRequest.getCreditLimit() != null && creditRequest.getCreditLimit() > 0) {
                return Mono.error(new IllegalArgumentException("Debit cards cannot have credit limit"));
            }
            if (creditRequest.getTermMonths() != null && creditRequest.getTermMonths() > 0) {
                return Mono.error(new IllegalArgumentException("Debit cards cannot have term months"));
            }
            if (creditRequest.getMonthlyPayment() != null && creditRequest.getMonthlyPayment() > 0) {
                return Mono.error(new IllegalArgumentException("Debit cards cannot have monthly payment"));
            }
            if (creditRequest.getAmount() != null && creditRequest.getAmount() > 0) {
                return Mono.error(new IllegalArgumentException("Debit cards cannot have amount (use daily limits instead)"));
            }
        }

        log.debug("Business rules validation passed for customer: {}", creditRequest.getCustomerId());
        return Mono.just(creditRequest);
    }




    private Mono<Credit> validateThirdPartyPayment(Credit credit, ThirdPartyPaymentRequest request) {
        // Validar que el crédito esté activo
        if (!"ACTIVO".equals(credit.getStatus())) {
            return Mono.error(new IllegalArgumentException("Cannot make payment to inactive credit"));
        }

        // Validar que el monto no exceda el saldo pendiente
        if (request.getAmount() > credit.getOutstandingBalance()) {
            return Mono.error(new IllegalArgumentException(
                    String.format("Payment amount (%.2f) exceeds outstanding balance (%.2f)",
                            request.getAmount(), credit.getOutstandingBalance())
            ));
        }

        // Validar que el monto sea positivo
        if (request.getAmount() <= 0) {
            return Mono.error(new IllegalArgumentException("Payment amount must be greater than 0"));
        }

        // Validar que el pagador existe
        return customerServiceClient.getCustomerById(request.getPayerCustomerId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payer customer not found: " + request.getPayerCustomerId())))
                .then(Mono.just(credit));
    }

    private Mono<Credit> processThirdPartyPayment(Credit credit, ThirdPartyPaymentRequest request) {
        // Procesar el pago
        double newOutstandingBalance = credit.getOutstandingBalance() - request.getAmount();
        credit.setOutstandingBalance(newOutstandingBalance);

        // Para tarjetas de crédito, actualizar crédito disponible
        if ("TARJETA_CREDITO".equals(credit.getCreditType())) {
            double newAvailableCredit = credit.getAvailableCredit() + request.getAmount();
            credit.setAvailableCredit(newAvailableCredit);
        }

        // Para préstamos, actualizar pagos restantes si corresponde
        if ("PRESTAMO_PERSONAL".equals(credit.getCreditType()) || "PRESTAMO_EMPRESARIAL".equals(credit.getCreditType())) {
            if (newOutstandingBalance <= 0) {
                credit.setStatus("PAGADO");
                credit.setRemainingPayments(0);
            } else if (credit.getRemainingPayments() != null && credit.getRemainingPayments() > 0) {
                credit.setRemainingPayments(credit.getRemainingPayments() - 1);
            }
        }

        credit.setUpdatedAt(Instant.now());

        return Mono.just(credit);
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

}