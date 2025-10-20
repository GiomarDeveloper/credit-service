package com.bank.credit.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.validation.constraints.*;
import java.time.Instant;

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
    @Pattern(regexp = "PRESTAMO_PERSONAL|PRESTAMO_EMPRESARIAL|TARJETA_CREDITO",
            message = "creditType must be PRESTAMO_PERSONAL, PRESTAMO_EMPRESARIAL or TARJETA_CREDITO")
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
}