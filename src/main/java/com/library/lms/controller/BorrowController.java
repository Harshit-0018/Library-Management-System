package com.library.lms.controller;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.service.BorrowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping("/borrow/{bookId}/{memberId}")
    public ResponseEntity<BorrowRecord> borrowBook(@PathVariable Long bookId, @PathVariable Long memberId) {
        BorrowRecord record = borrowService.borrowBook(bookId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @PostMapping("/return/{recordId}")
    public ResponseEntity<BorrowRecord> returnBook(@PathVariable Long recordId) {
        BorrowRecord record = borrowService.returnBook(recordId);
        return ResponseEntity.ok(record);
    }
}
