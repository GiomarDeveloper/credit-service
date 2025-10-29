package com.bank.credit.infrastructure.messaging;

import com.bank.credit.infrastructure.event.CreditBalanceResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos relacionados con créditos para Kafka.
 * <p>
 * Esta clase se encarga de publicar eventos de respuesta de validación
 * de saldo de crédito en el topic de Kafka correspondiente, permitiendo
 * la comunicación asíncrona con otros microservicios.
 * </p>
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditEventProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * Envía un evento de respuesta de validación de saldo de crédito.
   * <p>
   * Publica el evento en el topic "yanki.credit.balance.response" usando
   * el inquiryId como clave para garantizar el ordenamiento de mensajes
   * relacionados.
   * </p>
   *
   * @param event el evento de respuesta a enviar
   *
   */
  public void sendCreditBalanceResponse(CreditBalanceResponseEvent event) {
    kafkaTemplate.send("yanki.credit.balance.response", event.getInquiryId(), event)
      .addCallback(
        result -> log.info("✅ Credit balance response sent - InquiryId: {}", event.getInquiryId()),
        error -> log.error("❌ Failed to send credit balance response: {}", error.getMessage())
      );
  }
}