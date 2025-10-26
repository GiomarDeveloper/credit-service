package com.bank.credit.model.response;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String accountType;
    private String customerId;
    private Double balance;
    private Double maintenanceFee;
    private Integer monthlyTransactionLimit;
    private Integer currentMonthTransactions;
    private Integer fixedTermDay;
    private List<HolderInfo> holders;
    private List<HolderInfo> signatories;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Double minimumDailyAverage;
    private Integer freeTransactionLimit;
    private Double excessTransactionFee;
}