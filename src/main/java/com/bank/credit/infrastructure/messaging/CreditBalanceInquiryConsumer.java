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

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditBalanceInquiryConsumer {

    private final CreditService creditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "yanki.credit.balance.inquiry", groupId = "credit-service")
    public void consumeCreditBalanceInquiry(String message) {
        try {
            log.info("📨 Received raw message: {}", message);

            // Deserializar manualmente desde String
            CreditBalanceInquiryEvent event = objectMapper.readValue(message, CreditBalanceInquiryEvent.class);

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
                                        result -> log.info("✅ Credit balance response sent - InquiryId: {}", event.getInquiryId()),
                                        error -> log.error("❌ Failed to send credit balance response: {}", error.getMessage())
                                );
                    });

        } catch (Exception e) {
            log.error("❌ Error processing credit balance inquiry: {}", e.getMessage(), e);
        }
    }
}