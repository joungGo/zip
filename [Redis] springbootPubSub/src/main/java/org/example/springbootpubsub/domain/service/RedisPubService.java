package org.example.springbootpubsub.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.redis.pub.RedisPublisher;
import org.example.springbootpubsub.redis.sub.RedisSubscribeListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPubService {

    private final RedisMessageListenerContainer redisMessageListenerContainer; // RedisMessageListenerContainer 는 Redis 의 메시지를 수신하는 리스너를 관리하는 컨테이너
    // Redis 의 메시지를 수신하는 리스너를 등록하고, 삭제하는 기능을 제공
    private final RedisPublisher redisPublisher; // RedisPublisher 는 Redis 채널에 메시지를 발행하는 역할

    // 각 Channel 별 Listener
    private final RedisSubscribeListener redisSubscribeListener; // RedisSubscribeListener 는 Redis 에서 발생하는 메시지를 수신하는 리스너
    // Redis 에서 발생하는 메시지를 수신하고, 해당 메시지를 처리하는 역할


    /**
     * Channel 별 Message 전송
     * reidsSubscribeListener 를 통해 Channel 을 구독하고, Message 전송
     * @param
     */
    public void pubMsgChannel(String channel, MessageDto message) {

        //1. 요청한 Channel 을 구독.
        redisMessageListenerContainer.addMessageListener(redisSubscribeListener, new ChannelTopic(channel));

        //2. Message 전송
        redisPublisher.publish(new ChannelTopic(channel), message);
    }

    /**
     * Channel 구독 취소
     *
     * @param channel
     *
     * removeMessageListener() : redisMessageListenerContainer 에서 MessageListener 를 제거
     */
    public void cancelSubChannel(String channel) {
        redisMessageListenerContainer.removeMessageListener(redisSubscribeListener);
    }
}