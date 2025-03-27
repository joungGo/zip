package org.example.springbootpubsub.redis.pub;

import org.example.springbootpubsub.domain.dto.MessageDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

// 발행자
@Service
public class RedisPublisher {

    //redis 로 메시지를 발행할 때 사용할  RedisTemplate 객체 선언
    //key
    private final RedisTemplate<String, Object> template;
    //생성자 주입
    public RedisPublisher(RedisTemplate<String, Object> template) {
        this.template = template;
    }


    /**
     * Object publish
     * ChannelTopic : 메시지를 발행할 Redis 의 채널(topic) 정보 :: 채널의 이름을 저장하고(), 채널의 이름을 가져오는(getTopic) 기능을 제공
     * MessageDto :  실제 보낼 메시지 객체
     * convertAndSend :  자바객체나 문자열을 Redis 가 전송할 수 있는 byte 형태로 반환
     * 메시지를 직렬화해서 지정한 Redis 채널에 발행  (연속적인 바이트 형태로 변환하는 과정 <- 직렬화)
     */
    public void publish(ChannelTopic topic, MessageDto dto) {
        template.convertAndSend(topic.getTopic(), dto); // convertAndSend(topic.getTopic(), dto) : dto 객체를 직렬화해서 지정한 Redis 채널에 발행
        // https://grok.com/share/bGVnYWN5_d3a90bd8-944e-4087-9b17-78fbfa815d11
    }

    /**
     * String publish
     */
    public void publish(ChannelTopic topic ,String data) {
        template.convertAndSend(topic.getTopic(), data);
    }
}