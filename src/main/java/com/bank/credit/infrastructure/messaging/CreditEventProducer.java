package com.bank.credit.infrastructure.messaging;

import com.bank.credit.infrastructure.event.CreditBalanceResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCreditBalanceResponse(CreditBalanceResponseEvent event) {
        kafkaTemplate.send("yanki.credit.balance.response", event.getInquiryId(), event)
                .addCallback(
                        result -> log.info("✅ Credit balance response sent - InquiryId: {}", event.getInquiryId()),
                        error -> log.error("❌ Failed to send credit balance response: {}", error.getMessage())
                );
    }
}