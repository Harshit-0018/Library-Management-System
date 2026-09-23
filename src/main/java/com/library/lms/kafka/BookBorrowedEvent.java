package com.library.lms.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Event payload published to the "book-events" Kafka topic whenever a book is borrowed.
 * Kept as a simple, flat, JSON-serializable POJO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookBorrowedEvent implements Serializable {

    private Long borrowRecordId;
    private Long bookId;
    private String bookTitle;
    private Long memberId;
    private String memberName;
    private String borrowDate;
}
