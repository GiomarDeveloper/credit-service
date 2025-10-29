package com.bank.credit.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entidad principal que representa un producto de crédito en el sistema.
 * <p>
 * Esta clase mapea a la colección "credits" en MongoDB y contiene toda la información
 * relacionada con un crédito, incluyendo datos básicos, límites, estado y cuentas asociadas.
 * Soporta diferentes tipos de crédito como préstamos personales, empresariales y tarjetas.
 * </p>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credits")
public class Credit {
  @Id
  private String id;

  @NotBlank(message = "creditNumber is required")
  @Size(min = 10, max = 20, message = "creditNumber must be between 10 and 20 characters")
  private String creditNumber;

  @NotBlank(message = "creditType is required")
  @Pattern(regexp = "PRESTAMO_PERSONAL|PRESTAMO_EMPRESARIAL|TARJETA_CREDITO|TARJETA_DEBITO",
    message = "creditType must be PRESTAMO_PERSONAL, PRESTAMO_EMPRESARIAL, TARJETA_CREDITO or TARJETA_DEBITO")
  private String creditType;

  @NotBlank(message = "customerId is required")
  private String customerId;

  @NotNull(message = "amount is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
  private Double amount;

  @NotNull(message = "outstandingBalance is required")
  @DecimalMin(value = "0.0", message = "outstandingBalance must be greater than or equal to 0")
  private Double outstandingBalance;

  @NotNull(message = "interestRate is required")
  @DecimalMin(value = "0.0", message = "interestRate must be greater than or equal to 0")
  @DecimalMax(value = "100.0", message = "interestRate must be less than or equal to 100")
  private Double interestRate;

  @DecimalMin(value = "0.0", message = "creditLimit must be greater than or equal to 0")
  private Double creditLimit;

  @DecimalMin(value = "0.0", message = "availableCredit must be greater than or equal to 0")
  private Double availableCredit;

  @Min(value = 1, message = "termMonths must be at least 1")
  private Integer termMonths;

  @DecimalMin(value = "0.0", message = "monthlyPayment must be greater than or equal to 0")
  private Double monthlyPayment;

  @Min(value = 0, message = "remainingPayments must be greater than or equal to 0")
  private Integer remainingPayments;

  @NotBlank(message = "status is required")
  @Pattern(regexp = "ACTIVO|INACTIVO|BLOQUEADO|PAGADO|MOROSO",
    message = "status must be ACTIVO, INACTIVO, BLOQUEADO, PAGADO or MOROSO")
  private String status;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @NotNull(message = "dueDate is required")
  private LocalDate dueDate;

  private String mainAccountId;

  private List<AssociatedAccount> associatedAccounts;

  @DecimalMin(value = "0.0", message = "dailyWithdrawalLimit must be greater than or equal to 0")
  private Double dailyWithdrawalLimit;

  @DecimalMin(value = "0.0", message = "dailyPurchaseLimit must be greater than or equal to 0")
  private Double dailyPurchaseLimit;

  private LocalDate expirationDate;

  private String cvv;

  @Pattern(regexp = "VISA|MASTERCARD", message = "cardBrand must be VISA or MASTERCARD")
  private String cardBrand;

  @Pattern(regexp = "ACTIVA|INACTIVA|BLOQUEADA|REPORTADA_ROBADA|VENCIDA",
    message = "cardStatus must be ACTIVA, INACTIVA, BLOQUEADA, REPORTADA_ROBADA or VENCIDA")
  private String cardStatus;

  private Instant activatedAt;

  @Min(value = 0, message = "dailyTransactionsCount must be greater than or equal to 0")
  private Integer dailyTransactionsCount;

  @DecimalMin(value = "0.0", message = "dailyTransactionsAmount must be greater than or equal to 0")
  private Double dailyTransactionsAmount;

  private LocalDate lastDailyReset;
}