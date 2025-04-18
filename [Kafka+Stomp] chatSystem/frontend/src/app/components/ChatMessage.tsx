'use client';

import { ChatMessage as ChatMessageType, MessageType } from '@/types';
import { formatRelative } from 'date-fns';
import { ko } from 'date-fns/locale';

interface ChatMessageProps {
  message: ChatMessageType;
  isCurrentUser: boolean;
}

export function ChatMessage({ message, isCurrentUser }: ChatMessageProps) {
  const formattedTime = formatRelative(new Date(message.timestamp), new Date(), {
    locale: ko,
  });

  // 시스템 메시지 (Join, Leave)
  if (message.type !== MessageType.CHAT) {
    const action = message.type === MessageType.JOIN ? '입장' : '퇴장';
    return (
      <div className="text-center text-sm text-gray-500 my-2">
        <span className="font-medium">{message.sender}</span>님이 {action}했습니다.
        <div className="text-xs">{formattedTime}</div>
      </div>
    );
  }

  // 일반 채팅 메시지
  return (
    <div
      className={`flex flex-col my-2 max-w-[70%] ${
        isCurrentUser ? 'ml-auto items-end' : ''
      }`}
    >
      {!isCurrentUser && (
        <div className="text-sm font-medium mb-1">{message.sender}</div>
      )}
      <div
        className={`rounded-lg px-3 py-2 break-words ${
          isCurrentUser
            ? 'bg-blue-500 text-white'
            : 'bg-gray-200 text-gray-800'
        }`}
      >
        {message.content}
      </div>
      <div className="text-xs text-gray-500 mt-1">{formattedTime}</div>
    </div>
  );
} 