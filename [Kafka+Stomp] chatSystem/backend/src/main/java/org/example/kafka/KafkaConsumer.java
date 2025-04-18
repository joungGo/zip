package org.example.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatMessageDto;
import org.example.model.mongodb.ChatMessage;
import org.example.service.ChatMessageService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "${app.kafka.topics.chat-messages}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeChatMessage(ChatMessageDto message) {
        log.info("Received message from Kafka: {}", message);
        
        try {
            // 메시지 저장
            chatMessageService.saveMessage(message);
            
            // 실시간 전송 - 채팅방 구독자들에게 메시지 전달
            messagingTemplate.convertAndSend("/topic/chat/" + message.getRoomId(), message);
            
            log.info("Message processed and forwarded to WebSocket clients");
        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.chat-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeChatEvent(ChatMessageDto event) {
        log.info("Received event from Kafka: {}", event);
        
        try {
            // 이벤트 저장 (참여/퇴장 등)
            chatMessageService.saveMessage(event);
            
            // 실시간 전송 - 채팅방 구독자들에게 이벤트 전달
            messagingTemplate.convertAndSend("/topic/chat/" + event.getRoomId(), event);
            
            log.info("Event processed and forwarded to WebSocket clients");
        } catch (Exception e) {
            log.error("Error processing chat event: {}", e.getMessage(), e);
        }
    }
} 