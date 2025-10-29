package com.bank.credit.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreditValidationResult {
    private boolean isValid;
    private String reason;
    private BigDecimal availableBalance;
}