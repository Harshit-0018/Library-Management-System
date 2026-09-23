package com.library.lms.service;

import com.library.lms.entity.Book;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Member;
import com.library.lms.exception.BookNotAvailableException;
import com.library.lms.exception.BookNotFoundException;
import com.library.lms.exception.BorrowRecordNotFoundException;
import com.library.lms.exception.MemberNotFoundException;
import com.library.lms.kafka.BookBorrowedEvent;
import com.library.lms.kafka.BookEventProducer;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles the borrow / return workflow:
 *  - borrowBook: validates book & member, creates a BorrowRecord, flips the book to
 *    unavailable, and publishes a "book-borrowed" event to Kafka.
 *  - returnBook: closes out the BorrowRecord and flips the book back to available.
 */
@Slf4j
@Service
public class BorrowService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookEventProducer bookEventProducer;

    public BorrowService(BookRepository bookRepository,
                          MemberRepository memberRepository,
                          BorrowRecordRepository borrowRecordRepository,
                          BookEventProducer bookEventProducer) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookEventProducer = bookEventProducer;
    }

    @Transactional
    public BorrowRecord borrowBook(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> BookNotFoundException.forId(bookId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> MemberNotFoundException.forId(memberId));

        if (!book.isAvailable()) {
            throw BookNotAvailableException.forBook(bookId);
        }

        LocalDateTime now = LocalDateTime.now();

        BorrowRecord record = BorrowRecord.builder()
                .book(book)
                .member(member)
                .borrowDate(now)
                .returnDate(null)
                .build();
        BorrowRecord savedRecord = borrowRecordRepository.save(record);

        book.setAvailable(false);
        bookRepository.save(book);

        log.info("Book id={} borrowed by member id={} (recordId={})", bookId, memberId, savedRecord.getId());

        BookBorrowedEvent event = BookBorrowedEvent.builder()
                .borrowRecordId(savedRecord.getId())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .memberId(member.getId())
                .memberName(member.getName())
                .borrowDate(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        bookEventProducer.publishBookBorrowedEvent(event);

        return savedRecord;
    }

    @Transactional
    public BorrowRecord returnBook(Long recordId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> BorrowRecordNotFoundException.forId(recordId));

        if (record.getReturnDate() != null) {
            throw new IllegalArgumentException("Borrow record with id " + recordId + " has already been returned");
        }

        record.setReturnDate(LocalDateTime.now());
        BorrowRecord updatedRecord = borrowRecordRepository.save(record);

        Book book = record.getBook();
        book.setAvailable(true);
        bookRepository.save(book);

        log.info("Book id={} returned (recordId={})", book.getId(), recordId);

        return updatedRecord;
    }
}
