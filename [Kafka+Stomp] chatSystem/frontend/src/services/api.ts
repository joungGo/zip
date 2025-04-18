import axios from 'axios';
import { ChatRoom, ChatMessage } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_BASE_URL
});

export const chatApi = {
    // 채팅방 관련 API
    getAllRooms: async (): Promise<ChatRoom[]> => {
        const response = await api.get('/rooms');
        return response.data;
    },

    getUserRooms: async (username: string): Promise<ChatRoom[]> => {
        const response = await api.get(`/rooms/user/${username}`);
        return response.data;
    },

    getRoomById: async (roomId: number): Promise<ChatRoom> => {
        const response = await api.get(`/rooms/${roomId}`);
        return response.data;
    },

    createRoom: async (roomName: string, creator: string): Promise<ChatRoom> => {
        const response = await api.post('/rooms', { roomName, creator });
        return response.data;
    },

    joinRoom: async (roomId: number, username: string): Promise<void> => {
        await api.post(`/rooms/${roomId}/participants`, { username });
    },

    leaveRoom: async (roomId: number, username: string): Promise<void> => {
        await api.delete(`/rooms/${roomId}/participants/${username}`);
    },

    // 메시지 관련 API
    getRoomMessages: async (roomId: number): Promise<ChatMessage[]> => {
        const response = await api.get(`/messages/${roomId}`);
        return response.data;
    },

    getRecentMessages: async (roomId: number, limit: number = 50): Promise<ChatMessage[]> => {
        const response = await api.get(`/messages/${roomId}/recent`, {
            params: { limit }
        });
        return response.data;
    }
} 