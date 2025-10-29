package com.bank.credit.infrastructure.messaging;

import com.bank.credit.infrastructure.event.CreditBalanceInquiryEvent;
import com.bank.credit.infrastructure.event.CreditBalanceResponseEvent;
import com.bank.credit.service.CreditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de consulta de saldo de crédito desde Kafka.
 * <p>
 * Esta clase se encarga de escuchar y procesar mensajes del topic de Kafka
 * "yanki.credit.balance.inquiry", que contienen solicitudes de validación
 * de saldo de crédito para diversas operaciones financieras.
 * </p>
 * <p>
 * Al recibir un mensaje, deserializa el evento, valida el crédito a través
 * del servicio correspondiente y envía una respuesta al topic de respuestas.
 * </p>
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditBalanceInquiryConsumer {

  private final CreditService creditService;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Procesa mensajes de consulta de saldo de crédito desde Kafka.
   * <p>
   * Este método escucha el topic "yanki.credit.balance.inquiry" y procesa
   * cada mensaje para validar si un crédito tiene saldo suficiente para
   * una operación específica.
   * </p>
   *
   * @param message el mensaje JSON serializado como String
   *
   */
  @KafkaListener(topics = "yanki.credit.balance.inquiry", groupId = "credit-service")
  public void consumeCreditBalanceInquiry(String message) {
    try {
      log.info("📨 Received raw message: {}", message);

      // Deserializar manualmente desde String
      CreditBalanceInquiryEvent event =
        objectMapper.readValue(message, CreditBalanceInquiryEvent.class);

      log.info("🎯 Parsed credit balance inquiry - InquiryId: {}, CreditId: {}, Amount: {}",
        event.getInquiryId(), event.getCreditId(), event.getRequiredAmount());

      // Procesar la validación
      creditService.validateCreditForTransaction(event.getCreditId(), event.getRequiredAmount())
        .subscribe(validationResult -> {
          // Enviar respuesta
          CreditBalanceResponseEvent responseEvent = CreditBalanceResponseEvent.builder()
            .inquiryId(event.getInquiryId())
            .creditId(event.getCreditId())
            .isValid(validationResult.isValid())
            .reason(validationResult.getReason())
            .availableBalance(validationResult.getAvailableBalance())
            .transactionId(event.getTransactionId())
            .build();

          kafkaTemplate.send("yanki.credit.balance.response", event.getInquiryId(), responseEvent)
            .addCallback(
              result -> log.info("✅ Credit balance response sent - InquiryId: {}",
                event.getInquiryId()),
              error -> log.error("❌ Failed to send credit balance response: {}", error.getMessage())
            );
        });

    } catch (Exception e) {
      log.error("❌ Error processing credit balance inquiry: {}", e.getMessage(), e);
    }
  }
}