package com.example.websockettest;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocketHandler는 클라이언트와의 WebSocket 연결을 처리하는 클래스입니다.
 * 클라이언트로부터 메시지를 받고, 응답 메시지를 보내는 기능을 구현합니다.
 */
public class WebSocketHandler extends TextWebSocketHandler {

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 클라이언트로부터 받은 메시지
        String payload = message.getPayload();
        System.out.println("Received: " + payload);

        // 클라이언트에게 응답 보내기
        session.sendMessage(new TextMessage("Hello, " + payload + "!"));
    }
}