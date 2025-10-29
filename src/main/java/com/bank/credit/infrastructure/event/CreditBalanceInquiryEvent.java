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
public class CreditBalanceInquiryEvent {
    private String inquiryId;
    private String creditId;
    private BigDecimal requiredAmount;
    private String source;
    private String transactionId;
    private String fromPhoneNumber;
    private String toPhoneNumber;
    private String description;
}