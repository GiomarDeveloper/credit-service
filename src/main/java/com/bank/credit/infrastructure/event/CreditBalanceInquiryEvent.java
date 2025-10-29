package com.bank.credit.infrastructure.event;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de consulta de saldo de crédito para comunicación entre microservicios.
 * <p>
 * Este evento representa una solicitud de consulta de saldo de crédito que puede ser
 * publicada en un bus de eventos (como Kafka) para ser consumida por otros microservicios
 * que necesiten verificar la disponibilidad de fondos en un crédito.
 * </p>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBalanceInquiryEvent {
  private String inquiryId;
  private String creditId;
  private BigDecimal requiredAmount;
  private String source;
  private String transactionId;
  private String fromPhoneNumber;
  private String toPhoneNumber;
  private String description;
}