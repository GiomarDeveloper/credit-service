package com.bank.credit.service;

import com.bank.credit.model.ConsumptionRequest;
import com.bank.credit.model.CreditBalanceResponse;
import com.bank.credit.model.CreditDailyBalance;
import com.bank.credit.model.CreditRequest;
import com.bank.credit.model.CreditResponse;
import com.bank.credit.model.CreditTypeEnum;
import com.bank.credit.model.CreditValidationResult;
import com.bank.credit.model.DebitCardMainAccountBalanceResponse;
import com.bank.credit.model.PaymentRequest;
import com.bank.credit.model.ThirdPartyPaymentRequest;
import java.math.BigDecimal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditService {
  Flux<CreditResponse> getAllCredits(String customerId, String creditType);

  Mono<CreditResponse> getCreditById(String id);

  Mono<CreditResponse> createCredit(CreditRequest creditRequest);

  Mono<CreditResponse> updateCredit(String id, CreditRequest creditRequest);

  Mono<Void> deleteCredit(String id);

  Flux<CreditResponse> getCreditsByCustomer(String customerId, CreditTypeEnum creditType);

  Mono<CreditBalanceResponse> getCreditBalance(String id);

  Mono<CreditResponse> makePayment(String id, PaymentRequest paymentRequest);

  Mono<CreditResponse> chargeConsumption(String id, ConsumptionRequest consumptionRequest);

  Flux<CreditDailyBalance> getCustomerCreditsWithDailyBalances(String customerId);

  Mono<Boolean> hasOverdueCredits(String customerId);

  Mono<CreditResponse> makeThirdPartyPayment(String creditId, ThirdPartyPaymentRequest request);

  Mono<DebitCardMainAccountBalanceResponse> getDebitCardMainAccountBalance(String cardId);

  Mono<CreditValidationResult> validateCreditForTransaction(String creditId,
                                                            BigDecimal requiredAmount);
}