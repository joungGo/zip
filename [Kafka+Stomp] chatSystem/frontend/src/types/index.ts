export interface ChatRoom {
    id: number;
    roomName: string;
    createdAt: string;
    participants: string[];
}

export interface ChatMessage {
    roomId: number;
    sender: string;
    content: string;
    type: MessageType;
    timestamp: string;
}

export enum MessageType {
    CHAT = 'CHAT',
    JOIN = 'JOIN',
    LEAVE = 'LEAVE'
}