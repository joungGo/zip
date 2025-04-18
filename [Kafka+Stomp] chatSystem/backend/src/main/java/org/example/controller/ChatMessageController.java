package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatMessageDto;
import org.example.kafka.KafkaProducer;
import org.example.model.mongodb.ChatMessage;
import org.example.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final KafkaProducer kafkaProducer;

    /**
     * WebSocket을 통해 들어오는 메시지 처리
     * /app/chat/send/{roomId}로 들어오는 메시지를 처리
     */
    @MessageMapping("/chat/send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload ChatMessageDto message) {
        log.info("Received message via WebSocket for room {}: {}", roomId, message);
        
        // 메시지에 방 ID와 시간 설정
        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());
        
        // 메시지 타입에 따라 다른 Kafka 토픽으로 전송
        if (message.getType() == ChatMessage.MessageType.CHAT) {
            kafkaProducer.sendChatMessage(message);
        } else {
            kafkaProducer.sendChatEvent(message);
        }
    }

    /**
     * 특정 채팅방의 메시지 기록 조회 (REST API)
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(@PathVariable Long roomId) {
        List<ChatMessageDto> messages = chatMessageService.getAllMessages(roomId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 채팅방의 최근 메시지 조회 (REST API)
     */
    @GetMapping("/{roomId}/recent")
    public ResponseEntity<List<ChatMessageDto>> getRecentRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<ChatMessageDto> messages = chatMessageService.getRecentMessages(roomId, limit);
        return ResponseEntity.ok(messages);
    }
} 