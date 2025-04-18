package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatRoomDto;
import org.example.model.mysql.ChatRoom;
import org.example.repository.mysql.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getAllChatRooms() {
        return chatRoomRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(String username) {
        return chatRoomRepository.findByParticipantsContaining(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoomDto> getChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .map(this::convertToDto);
    }

    @Transactional
    public ChatRoomDto createChatRoom(String roomName, String creator) {
        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(roomName)
                .participants(new HashSet<>())
                .build();
        
        chatRoom.getParticipants().add(creator);
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        return convertToDto(savedRoom);
    }

    @Transactional
    public boolean addUserToChatRoom(Long roomId, String username) {
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findById(roomId);
        
        if (optionalChatRoom.isPresent()) {
            ChatRoom chatRoom = optionalChatRoom.get();
            chatRoom.getParticipants().add(username);
            chatRoomRepository.save(chatRoom);
            return true;
        }
        
        return false;
    }

    @Transactional
    public boolean removeUserFromChatRoom(Long roomId, String username) {
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findById(roomId);
        
        if (optionalChatRoom.isPresent()) {
            ChatRoom chatRoom = optionalChatRoom.get();
            boolean removed = chatRoom.getParticipants().remove(username);
            
            if (removed) {
                // If no participants left, delete the room
                if (chatRoom.getParticipants().isEmpty()) {
                    chatRoomRepository.delete(chatRoom);
                } else {
                    chatRoomRepository.save(chatRoom);
                }
                return true;
            }
        }
        
        return false;
    }

    private ChatRoomDto convertToDto(ChatRoom chatRoom) {
        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .roomName(chatRoom.getRoomName())
                .createdAt(chatRoom.getCreatedAt())
                .participants(chatRoom.getParticipants())
                .build();
    }
} 