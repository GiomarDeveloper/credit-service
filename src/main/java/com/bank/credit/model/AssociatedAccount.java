package com.bank.credit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

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