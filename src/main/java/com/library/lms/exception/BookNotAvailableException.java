package com.library.lms.exception;

/**
 * Thrown when an attempt is made to borrow a Book that is currently unavailable.
 */
public class BookNotAvailableException extends RuntimeException {

    public BookNotAvailableException(String message) {
        super(message);
    }

    public static BookNotAvailableException forBook(Long bookId) {
        return new BookNotAvailableException("Book with id " + bookId + " is not available for borrowing");
    }
}
