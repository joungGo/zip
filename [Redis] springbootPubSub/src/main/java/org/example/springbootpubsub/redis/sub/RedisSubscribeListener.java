package org.example.springbootpubsub.redis.sub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscribeListener implements MessageListener { // MessageListener 는 Redis 에서 발생하는 메시지를 수신하는 리스너

    private final RedisTemplate<String, Object> template;
    private final ObjectMapper objectMapper; // JSON <-> Object 변환 :: ObjectMapper 는 JSON 데이터를 Java Object 로 변환하거나, Java Object 를 JSON 데이터로 변환하는 기능을 제공

    @Override
    public void onMessage(Message message, byte[] pattern) { // Message: Redis 에서 발생한 메시지, pattern: 메시지를 발행한 채널의 이름
        try {
            String publishMessage = template
                    .getStringSerializer().deserialize(message.getBody()); // Redis 에서 발생한 메시지를 String 으로 변환
                    // getStringSerializer() : RedisTemplate 의 StringSerializer 를 가져옴
                    // StringSerializer : RedisTemplate 의 KeySerializer, ValueSerializer 류를 설정하는데 사용
                    // deserialize() : byte[] 를 String 으로 변환

            MessageDto messageDto = objectMapper.readValue(publishMessage, MessageDto.class);
            // readValue() : JSON 데이터를 Java Object 로 변환 -> publishMessage : JSON 데이터, MessageDto.class : 변환할 Java Object

            log.info("Redis Subscribe Channel : " + messageDto.getRoomId());
            log.info("Redis SUB Message : {}", publishMessage);

            // Return || Another Method Call(etc.save to DB)
            // TODO
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}