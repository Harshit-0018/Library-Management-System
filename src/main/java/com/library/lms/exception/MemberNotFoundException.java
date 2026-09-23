package com.library.lms.exception;

/**
 * Thrown when a requested Member cannot be found by its identifier.
 */
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(String message) {
        super(message);
    }

    public static MemberNotFoundException forId(Long id) {
        return new MemberNotFoundException("Member not found with id: " + id);
    }
}
