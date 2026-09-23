package com.library.lms.exception;

/**
 * Thrown when a requested Book cannot be found by its identifier.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(String message) {
        super(message);
    }

    public static BookNotFoundException forId(Long id) {
        return new BookNotFoundException("Book not found with id: " + id);
    }
}
