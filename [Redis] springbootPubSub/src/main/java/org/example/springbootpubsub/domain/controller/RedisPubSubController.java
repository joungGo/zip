package org.example.springbootpubsub.domain.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.domain.service.RedisPubService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/redis/pubsub")
public class RedisPubSubController {

    private final RedisPubService redisSubscribeService;


    @PostMapping("/send") //특정 redis 채널에 메세지를 발행하는 API
    public void sendMessage(@RequestParam(required = true) String channel, @RequestBody MessageDto message) {
        log.info("Redis Pub MSG Channel = {}", channel);
        redisSubscribeService.pubMsgChannel(channel, message);
    }

    @PostMapping("/cancle") // 특정  redis 채널의 구독 해제 하는  API
    public void cancelSubChannel(@RequestParam String channel) {
        redisSubscribeService.cancelSubChannel(channel);
    }
}