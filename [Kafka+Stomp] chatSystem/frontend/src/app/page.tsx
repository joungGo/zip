'use client';

import { useState, useEffect } from 'react';
import { ChatRoomList } from './components/ChatRoomList';
import { ChatRoom } from './components/ChatRoom';
import { ChatRoom as ChatRoomType } from '@/types';
import { webSocketService } from '@/services/websocket';

export default function Home() {
  const [username, setUsername] = useState<string>('');
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoomType | null>(null);

  useEffect(() => {
    // 로컬 스토리지에서 사용자 이름 가져오기
    const savedUsername = localStorage.getItem('chat_username');
    if (savedUsername) {
      setUsername(savedUsername);
      setIsLoggedIn(true);
      // 웹소켓 연결
      webSocketService.connect(savedUsername);
    }
  }, []);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!username.trim()) return;
    
    // 로컬 스토리지에 사용자 이름 저장
    localStorage.setItem('chat_username', username);
    setIsLoggedIn(true);
    
    // 웹소켓 연결
    webSocketService.connect(username);
  };

  const handleLogout = () => {
    // 웹소켓 연결 해제
    webSocketService.disconnect();
    
    // 로컬 스토리지에서 사용자 이름 제거
    localStorage.removeItem('chat_username');
    
    setIsLoggedIn(false);
    setSelectedRoom(null);
  };

  const handleRoomSelect = (room: ChatRoomType) => {
    setSelectedRoom(room);
  };

  const handleLeaveRoom = () => {
    setSelectedRoom(null);
  };

  // 로그인 폼
  if (!isLoggedIn) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-lg shadow-md w-96">
          <h1 className="text-2xl font-bold mb-6 text-center">채팅 앱 로그인</h1>
          <form onSubmit={handleLogin}>
            <div className="mb-4">
              <label
                htmlFor="username"
                className="block text-sm font-medium mb-2"
              >
                사용자 이름
              </label>
              <input
                type="text"
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded"
                placeholder="사용자 이름을 입력하세요"
                required
              />
            </div>
            <button
              type="submit"
              className="w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600"
            >
              입장하기
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen">
      <div className="flex flex-col h-full">
        <div className="p-3 bg-blue-600 text-white flex justify-between items-center">
          <div className="font-bold">안녕하세요, {username}님!</div>
          <button
            onClick={handleLogout}
            className="px-2 py-1 bg-red-500 text-white rounded text-sm hover:bg-red-600"
          >
            로그아웃
          </button>
        </div>
        <ChatRoomList
          username={username}
          onRoomSelect={handleRoomSelect}
          selectedRoomId={selectedRoom?.id}
        />
      </div>

      <div className="flex-1">
        {selectedRoom ? (
          <ChatRoom
            room={selectedRoom}
            username={username}
            onLeaveRoom={handleLeaveRoom}
          />
        ) : (
          <div className="flex h-full items-center justify-center bg-gray-100">
            <div className="text-center text-gray-500">
              <p className="text-xl mb-2">채팅방을 선택하세요</p>
              <p className="text-sm">왼쪽 목록에서 채팅방을 선택하거나 새로운 채팅방을 만드세요.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
