'use client';

import { useState, useEffect, useRef } from 'react';
import { ChatMessage as ChatMessageComponent } from './ChatMessage';
import { ChatMessage, ChatRoom as ChatRoomType, MessageType } from '@/types';
import { chatApi } from '@/services/api';
import { webSocketService } from '@/services/websocket';

interface ChatRoomProps {
  room: ChatRoomType;
  username: string;
  onLeaveRoom: () => void;
}

export function ChatRoom({ room, username, onLeaveRoom }: ChatRoomProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // 채팅방 입장 시 이전 메시지 로드
    loadMessages();

    // 웹소켓으로 방에 입장
    webSocketService.joinRoom(room.id);

    // 새 메시지 구독
    const unsubscribe = webSocketService.subscribe(handleNewMessage);

    return () => {
      unsubscribe();
      webSocketService.leaveRoom();
    };
  }, [room.id]);

  // 새 메시지가 오면 스크롤을 아래로 이동
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadMessages = async () => {
    try {
      setIsLoading(true);
      const recentMessages = await chatApi.getRecentMessages(room.id, 50);
      setMessages(recentMessages.reverse()); // 최신 메시지가 아래에 오도록 정렬
    } catch (error) {
      console.error('메시지 로드에 실패했습니다:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleNewMessage = (message: ChatMessage) => {
    setMessages((prevMessages) => [...prevMessages, message]);
  };

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!inputMessage.trim()) return;
    
    webSocketService.sendMessage(inputMessage);
    setInputMessage('');
  };

  const handleLeaveRoom = async () => {
    try {
      await chatApi.leaveRoom(room.id, username);
      onLeaveRoom();
    } catch (error) {
      console.error('채팅방 나가기에 실패했습니다:', error);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="flex flex-col h-screen">
      {/* 채팅방 헤더 */}
      <div className="bg-white p-4 border-b flex justify-between items-center">
        <div>
          <h2 className="text-xl font-bold">{room.roomName}</h2>
          <div className="text-sm text-gray-500">
            {room.participants.length}명 참여 중
          </div>
        </div>
        <button
          onClick={handleLeaveRoom}
          className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600"
        >
          나가기
        </button>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 overflow-y-auto p-4 bg-gray-50">
        {isLoading ? (
          <div className="flex justify-center items-center h-full">
            <div className="text-gray-500">메시지 로딩 중...</div>
          </div>
        ) : (
          <>
            {messages.map((message, index) => (
              <ChatMessageComponent
                key={index}
                message={message}
                isCurrentUser={message.sender === username}
              />
            ))}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* 메시지 입력 영역 */}
      <div className="bg-white p-4 border-t">
        <form onSubmit={sendMessage} className="flex">
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            placeholder="메시지를 입력하세요..."
            className="flex-1 p-2 border border-gray-300 rounded-l"
          />
          <button
            type="submit"
            className="bg-blue-500 text-white px-4 py-2 rounded-r hover:bg-blue-600"
          >
            전송
          </button>
        </form>
      </div>
    </div>
  );
} 