package com.bank.credit.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para información de transacciones financieras.
 * <p>
 * Representa una transacción financiera realizada en cualquier producto
 * del sistema (cuentas, créditos, etc.).
 * </p>
 *
 */
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