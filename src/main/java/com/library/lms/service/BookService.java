package com.library.lms.service;

import com.library.lms.dto.BookRequest;
import com.library.lms.entity.Book;
import com.library.lms.exception.BookNotFoundException;
import com.library.lms.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public Book createBook(BookRequest request) {
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .available(true)
                .build();
        Book saved = bookRepository.save(book);
        log.info("Created book id={} title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> BookNotFoundException.forId(id));
    }

    @Transactional
    public Book updateBook(Long id, BookRequest request) {
        Book book = getBookById(id);
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        Book updated = bookRepository.save(book);
        log.info("Updated book id={}", updated.getId());
        return updated;
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = getBookById(id);
        bookRepository.delete(book);
        log.info("Deleted book id={}", id);
    }
}
