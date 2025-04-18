package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.mongodb.ChatMessage;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long roomId;
    private String sender;
    private String content;
    private ChatMessage.MessageType type;
    private LocalDateTime timestamp;
} 