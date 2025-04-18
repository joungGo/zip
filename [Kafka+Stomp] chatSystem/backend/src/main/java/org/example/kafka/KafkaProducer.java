package org.example.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;

    @Value("${app.kafka.topics.chat-messages}")
    private String chatMessagesTopic;

    @Value("${app.kafka.topics.chat-events}")
    private String chatEventsTopic;

    public void sendChatMessage(ChatMessageDto message) {
        try {
            kafkaTemplate.send(chatMessagesTopic, message);
            log.info("Message sent to Kafka topic '{}': {}", chatMessagesTopic, message);
        } catch (Exception e) {
            log.error("Error sending message to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }

    public void sendChatEvent(ChatMessageDto event) {
        try {
            kafkaTemplate.send(chatEventsTopic, event);
            log.info("Event sent to Kafka topic '{}': {}", chatEventsTopic, event);
        } catch (Exception e) {
            log.error("Error sending event to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }
} 