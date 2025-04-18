package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatMessageDto;
import org.example.model.mongodb.ChatMessage;
import org.example.repository.mongodb.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public void saveMessage(ChatMessageDto messageDto) {
        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(messageDto.getRoomId())
                .sender(messageDto.getSender())
                .content(messageDto.getContent())
                .type(messageDto.getType())
                .createdAt(LocalDateTime.now())
                .build();
        
        chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessageDto> getRecentMessages(Long roomId, int limit) {
        Page<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, 
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        
        return messages.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> getAllMessages(Long roomId) {
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
        
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ChatMessageDto convertToDto(ChatMessage chatMessage) {
        return ChatMessageDto.builder()
                .roomId(chatMessage.getRoomId())
                .sender(chatMessage.getSender())
                .content(chatMessage.getContent())
                .type(chatMessage.getType())
                .timestamp(chatMessage.getCreatedAt())
                .build();
    }
} 