package com.library.lms.exception;

/**
 * Thrown when a requested BorrowRecord cannot be found by its identifier.
 * (Needed by the /return/{recordId} endpoint in addition to the core exceptions.)
 */
public class BorrowRecordNotFoundException extends RuntimeException {

    public BorrowRecordNotFoundException(String message) {
        super(message);
    }

    public static BorrowRecordNotFoundException forId(Long id) {
        return new BorrowRecordNotFoundException("Borrow record not found with id: " + id);
    }
}
