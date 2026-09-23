package com.library.lms.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "book-events" Kafka topic and logs a notification message
 * whenever a book-borrowed event is received.
 */
@Slf4j
@Component
public class BookEventConsumer {

    @KafkaListener(
            topics = "${app.kafka.topic.book-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(BookBorrowedEvent event) {
        log.info("Notification: Book '{}' borrowed by Member '{}'", event.getBookTitle(), event.getMemberName());
    }
}
