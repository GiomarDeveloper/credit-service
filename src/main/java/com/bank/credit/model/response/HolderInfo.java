package com.bank.credit.model.response;

import lombok.Data;

/**
 * DTO para información de titular o firmante de cuenta.
 * <p>
 * Representa la información básica de un titular o persona autorizada
 * para operar en una cuenta bancaria.
 * </p>
 *
 */
@Data
public class HolderInfo {
  private String customerId;
  private String name;
  private String relationship;
}