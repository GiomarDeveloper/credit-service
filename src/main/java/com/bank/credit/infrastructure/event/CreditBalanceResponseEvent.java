package com.bank.credit.infrastructure.event;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de respuesta de consulta de saldo de crédito.
 * <p>
 * Este evento representa la respuesta a una consulta de saldo de crédito,
 * indicando si la operación es válida basada en el saldo disponible y
 * proporcionando información adicional sobre el resultado de la validación.
 * </p>
 *
 */
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