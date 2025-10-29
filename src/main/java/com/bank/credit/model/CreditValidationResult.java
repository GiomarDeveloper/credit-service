package com.bank.credit.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Resultado de la validación de un crédito para una transacción.
 * <p>
 * Esta clase encapsula el resultado de validar si un crédito puede
 * ser utilizado para una operación específica, incluyendo la disponibilidad
 * de fondos y razones de rechazo si aplica.
 * </p>
 *
 */
@Data
@AllArgsConstructor
public class CreditValidationResult {
  private boolean isValid;
  private String reason;
  private BigDecimal availableBalance;
}