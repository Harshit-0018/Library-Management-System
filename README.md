# Library Management System

A backend service for managing a small library's catalog, members, and borrow/return
workflow — built with Spring Boot, PostgreSQL, and Kafka.

When a book is borrowed, the service publishes an event to Kafka, which is consumed
asynchronously to log a notification — demonstrating a simple event-driven flow
alongside a standard REST/JPA CRUD backend.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Running Locally with Docker Compose](#running-locally-with-docker-compose)
- [Running Without Docker](#running-without-docker-optional)
- [API Endpoints](#api-endpoints)
- [Error Handling](#error-handling)
- [Data Model](#data-model)

## Overview

Core features:

- **Book catalog management** — create, list, fetch, update, and delete books.
- **Member management** — register members and list them.
- **Borrowing workflow** — borrow a book (creates a `BorrowRecord`, flips the book to
  unavailable, and publishes a `book-events` Kafka message) and return a book (closes
  the record and flips the book back to available).
- **Centralized error handling** — a `@RestControllerAdvice` translates domain
  exceptions into consistent JSON error responses with correct HTTP status codes.

## Tech Stack

| Concern              | Technology                          |
|-----------------------|--------------------------------------|
| Language / Runtime    | Java 21                              |
| Framework             | Spring Boot 3.3.x                    |
| Build tool             | Maven                                |
| Web layer             | Spring Web (REST)                    |
| Persistence            | Spring Data JPA + PostgreSQL         |
| Messaging               | Apache Kafka (Spring Kafka)          |
| Containerization        | Docker, Docker Compose               |

## Architecture

```
                         HTTP (JSON)
   ┌────────┐    requests/responses    ┌──────────────┐
   │ Client │ ───────────────────────► │  Controller  │
   │ (curl, │ ◄─────────────────────── │   (REST)     │
   │Postman)│                          └──────┬───────┘
   └────────┘                                 │
                                               ▼
                                        ┌──────────────┐
                                        │   Service    │
                                        │ (business    │
                                        │  logic)      │
                                        └──────┬───────┘
                                               │
                                               ▼
                                        ┌──────────────┐
                                        │  Repository  │
                                        │ (Spring Data │
                                        │     JPA)     │
                                        └──────┬───────┘
                                               │
                                               ▼
                                        ┌──────────────┐
                                        │  PostgreSQL  │
                                        └──────────────┘

   Kafka event flow (triggered from BorrowService on a successful borrow):

   ┌───────────────┐  publish   ┌───────────────┐  consume   ┌───────────────┐
   │ BookEventProd │ ─────────► │ "book-events" │ ─────────► │ BookEventCons │
   │ (producer)    │            │     topic     │            │ (listener,    │
   └───────────────┘            └───────────────┘            │  logs message)│
                                                               └───────────────┘
```

Exceptions raised anywhere in the Service layer (`BookNotFoundException`,
`MemberNotFoundException`, `BookNotAvailableException`, etc.) bubble up to a
`GlobalExceptionHandler` (`@RestControllerAdvice`), which converts them into a
consistent JSON error body with the correct HTTP status code.

## Project Structure

```
library-management-system/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .gitignore
├── .dockerignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/library/lms/
    │   │   ├── LibraryManagementSystemApplication.java
    │   │   ├── controller/       # REST controllers (Book, Member, Borrow)
    │   │   ├── service/          # Business logic
    │   │   ├── repository/       # Spring Data JPA repositories
    │   │   ├── entity/           # JPA entities (Book, Member, BorrowRecord)
    │   │   ├── dto/               # Request DTOs with bean validation
    │   │   ├── kafka/             # Producer, consumer, event payload
    │   │   ├── exception/         # Custom exceptions + GlobalExceptionHandler
    │   │   └── config/            # Kafka topic configuration
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/library/lms/
            └── LibraryManagementSystemApplicationTests.java
```

## Running Locally with Docker Compose

This is the recommended way to run the whole stack — app, PostgreSQL, Zookeeper, and
Kafka — with a single command.

**Prerequisites:** Docker and Docker Compose installed.

```bash
# From the project root
docker-compose up --build
```

This will:

1. Build the Spring Boot app image using the multi-stage `Dockerfile`.
2. Start PostgreSQL, Zookeeper, and Kafka.
3. Wait for PostgreSQL and Kafka to report healthy, then start the app.
4. Expose the app on **http://localhost:8080**.

To stop everything:

```bash
docker-compose down
```

To stop and also wipe the PostgreSQL data volume:

```bash
docker-compose down -v
```

Tail the app logs (useful for watching the Kafka consumer log the borrow notification):

```bash
docker-compose logs -f app
```

## Running Without Docker (optional)

You'll need a local PostgreSQL instance and a local Kafka broker (with Zookeeper)
running, then:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=library_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

./mvnw spring-boot:run
```

(All of these environment variables have sensible `localhost` defaults baked into
`application.yml`, so you only need to set the ones that differ from your local setup.)

## API Endpoints

Base URL (local): `http://localhost:8080`

### Books

| Method | Path          | Description         |
|--------|---------------|----------------------|
| POST   | `/books`      | Create a new book     |
| GET    | `/books`      | List all books        |
| GET    | `/books/{id}` | Get a book by id       |
| PUT    | `/books/{id}` | Update a book          |
| DELETE | `/books/{id}` | Delete a book          |

**Create a book**

```
POST /books
Content-Type: application/json

{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884"
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "available": true
}
```

**List all books**

```
GET /books
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884",
    "available": true
  }
]
```

**Get a book by id**

```
GET /books/1
```

Response `200 OK`: same shape as above. Response `404 Not Found` if the id doesn't exist
(see [Error Handling](#error-handling)).

**Update a book**

```
PUT /books/1
Content-Type: application/json

{
  "title": "Clean Code (2nd Edition)",
  "author": "Robert C. Martin",
  "isbn": "9780132350884"
}
```

Response `200 OK`: the updated book.

**Delete a book**

```
DELETE /books/1
```

Response: `204 No Content`.

### Members

| Method | Path       | Description          |
|--------|------------|-----------------------|
| POST   | `/members` | Register a new member  |
| GET    | `/members` | List all members       |

**Create a member**

```
POST /members
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane.doe@example.com"
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "jane.doe@example.com"
}
```

**List all members**

```
GET /members
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  }
]
```

### Borrow / Return

| Method | Path                          | Description                             |
|--------|-------------------------------|-------------------------------------------|
| POST   | `/borrow/{bookId}/{memberId}` | Borrow a book on behalf of a member       |
| POST   | `/return/{recordId}`          | Return a previously borrowed book         |

**Borrow a book**

```
POST /borrow/1/1
```

Response `201 Created`:

```json
{
  "id": 1,
  "book": {
    "id": 1,
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884",
    "available": false
  },
  "member": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  },
  "borrowDate": "2026-09-23T10:15:30",
  "returnDate": null
}
```

Side effects:
- The book's `available` flag flips to `false`.
- A `book-events` Kafka message is published, e.g.:
  ```json
  {
    "borrowRecordId": 1,
    "bookId": 1,
    "bookTitle": "Clean Code",
    "memberId": 1,
    "memberName": "Jane Doe",
    "borrowDate": "2026-09-23T10:15:30"
  }
  ```
- The in-app Kafka consumer logs:
  ```
  Notification: Book 'Clean Code' borrowed by Member 'Jane Doe'
  ```

If the book is already borrowed, this returns `400 Bad Request` with a
`BookNotAvailableException` message.

**Return a book**

```
POST /return/1
```

Response `200 OK`:

```json
{
  "id": 1,
  "book": {
    "id": 1,
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884",
    "available": true
  },
  "member": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  },
  "borrowDate": "2026-09-23T10:15:30",
  "returnDate": "2026-09-24T09:00:00"
}
```

## Error Handling

All errors are returned as JSON with a consistent shape via a global
`@RestControllerAdvice` (`GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-09-23T10:20:00",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with id: 99",
  "path": "/books/99",
  "details": null
}
```

| Exception                       | HTTP Status | Trigger                                             |
|----------------------------------|-------------|-------------------------------------------------------|
| `BookNotFoundException`          | 404         | Book id doesn't exist                                  |
| `MemberNotFoundException`        | 404         | Member id doesn't exist                                |
| `BorrowRecordNotFoundException`  | 404         | Borrow record id doesn't exist                          |
| `BookNotAvailableException`      | 400         | Attempting to borrow a book that's already borrowed      |
| Validation errors (`@Valid`)     | 400         | Missing/invalid fields on create/update requests (`details` lists each field error) |
| Any other unhandled exception    | 500         | Unexpected server error                                  |

## Data Model

```
Book                Member               BorrowRecord
----                ------               ------------
id                  id                   id
title               name                 book       (→ Book)
author              email                member     (→ Member)
isbn                                     borrowDate
available                                returnDate (nullable)
```

- `BorrowRecord.book` and `BorrowRecord.member` are many-to-one relationships.
- `returnDate` is `null` while a book is out on loan, and set when it's returned.
