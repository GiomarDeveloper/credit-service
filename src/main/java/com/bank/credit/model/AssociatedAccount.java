package com.bank.credit.model;

import java.time.LocalDate;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo que representa una cuenta asociada a un producto de crédito.
 * <p>
 * Esta clase define la estructura de una cuenta bancaria que está vinculada
 * a un crédito, permitiendo operaciones como pagos automáticos, disposiciones
 * y otras transacciones relacionadas.
 * </p>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedAccount {
  @NotBlank
  private String accountId;

  @Min(1)
  private Integer sequenceOrder;

  private LocalDate associatedAt;

  private String status;
}