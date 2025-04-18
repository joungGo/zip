package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatRoomDto;
import org.example.service.ChatRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public ResponseEntity<List<ChatRoomDto>> getAllChatRooms() {
        List<ChatRoomDto> rooms = chatRoomService.getAllChatRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<ChatRoomDto>> getUserChatRooms(@PathVariable String username) {
        List<ChatRoomDto> rooms = chatRoomService.getUserChatRooms(username);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoomById(@PathVariable Long roomId) {
        return chatRoomService.getChatRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ChatRoomDto> createChatRoom(@RequestBody Map<String, String> request) {
        String roomName = request.get("roomName");
        String creator = request.get("creator");
        
        if (roomName == null || creator == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ChatRoomDto newRoom = chatRoomService.createChatRoom(roomName, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(newRoom);
    }

    @PostMapping("/{roomId}/participants")
    public ResponseEntity<Void> addUserToChatRoom(@PathVariable Long roomId, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        if (username == null) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean added = chatRoomService.addUserToChatRoom(roomId, username);
        
        if (added) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{roomId}/participants/{username}")
    public ResponseEntity<Void> removeUserFromChatRoom(@PathVariable Long roomId, @PathVariable String username) {
        boolean removed = chatRoomService.removeUserFromChatRoom(roomId, username);
        
        if (removed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 