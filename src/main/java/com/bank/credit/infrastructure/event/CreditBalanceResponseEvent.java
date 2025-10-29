package com.bank.credit.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBalanceResponseEvent {
    private String inquiryId;
    private String creditId;
    private Boolean isValid;
    private String reason;
    private BigDecimal availableBalance;
    private String transactionId;
}