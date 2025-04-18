'use client';

import { useState, useEffect } from 'react';
import { chatApi } from '@/services/api';
import { ChatRoom } from '@/types';
import { formatDistanceToNow } from 'date-fns';

interface ChatRoomListProps {
  username: string;
  onRoomSelect: (room: ChatRoom) => void;
  selectedRoomId?: number;
}

export function ChatRoomList({ username, onRoomSelect, selectedRoomId }: ChatRoomListProps) {
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [newRoomName, setNewRoomName] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = async () => {
    try {
      setIsLoading(true);
      const roomsList = await chatApi.getAllRooms();
      setRooms(roomsList);
    } catch (error) {
      console.error('채팅방 목록을 불러오는데 실패했습니다:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRoomName.trim()) return;

    try {
      const newRoom = await chatApi.createRoom(newRoomName, username);
      setRooms([newRoom, ...rooms]);
      setNewRoomName('');
    } catch (error) {
      console.error('채팅방 생성에 실패했습니다:', error);
    }
  };

  const handleRoomClick = (room: ChatRoom) => {
    onRoomSelect(room);
  };

  return (
    <div className="w-64 bg-gray-100 p-4 border-r border-gray-200 h-screen overflow-y-auto">
      <h2 className="text-xl font-bold mb-4">채팅방 목록</h2>
      
      <form onSubmit={handleCreateRoom} className="mb-4">
        <div className="flex">
          <input
            type="text"
            value={newRoomName}
            onChange={(e) => setNewRoomName(e.target.value)}
            placeholder="새 채팅방 이름"
            className="flex-1 p-2 border border-gray-300 rounded text-sm"
          />
          <button
            type="submit"
            className="ml-2 bg-blue-500 text-white px-2 py-1 rounded text-sm"
          >
            생성
          </button>
        </div>
      </form>

      <div className="space-y-2">
        {isLoading ? (
          <div className="text-center text-gray-500">로딩 중...</div>
        ) : rooms.length > 0 ? (
          rooms.map((room) => (
            <div
              key={room.id}
              className={`p-3 rounded cursor-pointer transition-colors ${
                selectedRoomId === room.id
                  ? 'bg-blue-100 border-blue-300 border'
                  : 'bg-white hover:bg-gray-50 border border-gray-200'
              }`}
              onClick={() => handleRoomClick(room)}
            >
              <div className="font-medium">{room.roomName}</div>
              <div className="text-sm text-gray-500 flex justify-between">
                <span>{room.participants.length}명 참여 중</span>
                <span>
                  {formatDistanceToNow(new Date(room.createdAt), {
                    addSuffix: true,
                  })}
                </span>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center text-gray-500 py-4">채팅방이 없습니다.</div>
        )}
      </div>
    </div>
  );
} 