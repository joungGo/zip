package org.example.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    @Field("room_id")
    private Long roomId;

    @Field("sender")
    private String sender;

    @Field("content")
    private String content;

    @Field("type")
    private MessageType type;

    @Field("created_at")
    private LocalDateTime createdAt;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
} 