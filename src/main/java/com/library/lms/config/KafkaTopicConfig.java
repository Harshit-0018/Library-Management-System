package com.library.lms.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Ensures the "book-events" Kafka topic exists on application startup.
 * Spring Boot auto-configures a KafkaAdmin bean from spring.kafka.bootstrap-servers,
 * which will use this NewTopic bean definition to create the topic if it does not exist.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.book-events}")
    private String bookEventsTopicName;

    @Bean
    public NewTopic bookEventsTopic() {
        return TopicBuilder.name(bookEventsTopicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
