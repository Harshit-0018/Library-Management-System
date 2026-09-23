package com.library.lms.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes book-related domain events to the "book-events" Kafka topic.
 * Currently used to notify consumers whenever a book is borrowed.
 */
@Slf4j
@Component
public class BookEventProducer {

    private final KafkaTemplate<String, BookBorrowedEvent> kafkaTemplate;
    private final String topic;

    public BookEventProducer(KafkaTemplate<String, BookBorrowedEvent> kafkaTemplate,
                              @Value("${app.kafka.topic.book-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishBookBorrowedEvent(BookBorrowedEvent event) {
        log.info("Publishing book-borrowed event to topic '{}': {}", topic, event);
        kafkaTemplate.send(topic, String.valueOf(event.getBookId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish book-borrowed event for bookId {}", event.getBookId(), ex);
                    } else {
                        log.debug("Book-borrowed event published successfully: partition={}, offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
