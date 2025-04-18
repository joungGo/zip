import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ChatMessage, MessageType } from '../types';

const SOCKET_URL = 'http://localhost:8080/ws';

export class WebSocketService {
    private client: Client | null = null;
    private connected: boolean = false;
    private username: string = '';
    private roomId: number | null = null;
    private messageHandlers: ((message: ChatMessage) => void)[] = [];

    constructor() {
        this.client = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: this.onConnect.bind(this),
            onDisconnect: this.onDisconnect.bind(this),
            onStompError: this.onError.bind(this)
        });
    }

    public connect(username: string): void {
        if (this.connected) return;
        
        this.username = username;
        
        if (this.client) {
            this.client.activate();
        }
    }

    public disconnect(): void {
        if (this.client && this.connected) {
            this.leaveRoom();
            this.client.deactivate();
            this.connected = false;
        }
    }

    public joinRoom(roomId: number): void {
        if (!this.connected || !this.client) return;
        
        // 이전 방에서 나가기
        this.leaveRoom();
        
        this.roomId = roomId;
        
        // 새 방 구독
        this.client.subscribe(`/topic/chat/${roomId}`, this.handleMessage.bind(this));
        
        // 입장 메시지 전송
        this.sendMessage('', MessageType.JOIN);
    }

    public leaveRoom(): void {
        if (!this.connected || !this.client || !this.roomId) return;
        
        // 퇴장 메시지 전송
        this.sendMessage('', MessageType.LEAVE);
        
        // 구독 해제 (프론트에서만)
        this.roomId = null;
    }

    public sendMessage(content: string, type: MessageType = MessageType.CHAT): void {
        if (!this.connected || !this.client || !this.roomId) return;
        
        const message: ChatMessage = {
            roomId: this.roomId,
            sender: this.username,
            content,
            type,
            timestamp: new Date().toISOString()
        };
        
        this.client.publish({
            destination: `/app/chat/send/${this.roomId}`,
            body: JSON.stringify(message)
        });
    }

    public subscribe(handler: (message: ChatMessage) => void): () => void {
        this.messageHandlers.push(handler);
        
        // Unsubscribe 함수 반환
        return () => {
            this.messageHandlers = this.messageHandlers.filter(h => h !== handler);
        };
    }

    private onConnect(): void {
        this.connected = true;
        console.log('Connected to WebSocket');
    }

    private onDisconnect(): void {
        this.connected = false;
        console.log('Disconnected from WebSocket');
    }

    private onError(frame: any): void {
        console.error('WebSocket error:', frame);
    }

    private handleMessage(message: IMessage): void {
        try {
            const chatMessage: ChatMessage = JSON.parse(message.body);
            
            // 모든 핸들러에게 메시지 전달
            this.messageHandlers.forEach(handler => handler(chatMessage));
        } catch (error) {
            console.error('Error parsing message:', error);
        }
    }
}

// 싱글톤 인스턴스 생성
export const webSocketService = new WebSocketService();