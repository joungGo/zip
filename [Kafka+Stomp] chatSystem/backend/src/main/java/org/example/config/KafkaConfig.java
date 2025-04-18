package org.example.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.example.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.chat-messages}")
    private String chatMessagesTopic;

    @Value("${app.kafka.topics.chat-events}")
    private String chatEventsTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name(chatMessagesTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatEventsTopic() {
        return TopicBuilder.name(chatEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
} 