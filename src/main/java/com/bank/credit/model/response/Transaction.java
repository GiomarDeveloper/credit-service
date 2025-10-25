package com.bank.credit.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String transactionType;
    private String productType;
    private String productId;
    private String customerId;
    private Double amount;
    private String description;
    private Double previousBalance;
    private Double newBalance;
    private String transactionDate;
    private String status;
}