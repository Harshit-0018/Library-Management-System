# Library Management System

A production-oriented backend service for managing a library's books, members, and borrow/return workflow, built with **Java 21, Spring Boot, PostgreSQL, Spring Data JPA, Apache Kafka, Maven, and Docker Compose**.

The project demonstrates a clean layered backend architecture and an event-driven workflow: when a book is successfully borrowed, the service persists the transaction, marks the book unavailable, and publishes a `BookBorrowedEvent` to Kafka. A Kafka consumer asynchronously receives the event and currently logs a notification. 

---


## Table of Contents

- [Overview](#overview)
- [Why This Project](#why-this-project)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [End-to-End Borrow Workflow](#end-to-end-borrow-workflow)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Domain Model](#domain-model)
- [API Reference](#api-reference)
- [Kafka Event Flow](#kafka-event-flow)
- [Validation and Error Handling](#validation-and-error-handling)
- [Running with Docker Compose](#running-with-docker-compose)
- [Running Locally Without Docker](#running-locally-without-docker)
- [Example API Flow](#example-api-flow)
- [Testing](#testing)
- [Design Decisions](#design-decisions)
- [Concurrency and Reliability Considerations](#concurrency-and-reliability-considerations)
- [Production Improvements](#production-improvements)
- [Interview Talking Points](#interview-talking-points)
- [Future Enhancements](#future-enhancements)
- [Author](#author)

---

## Overview

The Library Management System is a backend application designed around three core domain concepts:

1. **Books** — maintain the library catalog and current availability.
2. **Members** — register and manage library users.
3. **Borrow Records** — preserve the history of borrowing and returning transactions.

The application follows a **layered architecture**:

```text
Client / Postman / cURL
         |
         | HTTP + JSON
         v
+------------------------+
|      Controller        |
| REST API / HTTP layer  |
+-----------+------------+
            |
            v
+------------------------+
|        Service         |
| Business rules / txn   |
+-----------+------------+
            |
            v
+------------------------+
|       Repository       |
| Spring Data JPA        |
+-----------+------------+
            |
            v
+------------------------+
|      PostgreSQL        |
| Persistent data        |
+------------------------+

Successful borrow
        |
        v
+------------------------+
|   Kafka Producer       |
+-----------+------------+
            |
            v
+------------------------+
|  book-events topic     |
+-----------+------------+
            |
            v
+------------------------+
|   Kafka Consumer       |
| async event handling  |
+------------------------+
```

The central business operation is the **borrow workflow**:

```text
Validate book
    ↓
Validate member
    ↓
Check availability
    ↓
Create BorrowRecord
    ↓
Persist borrow transaction
    ↓
Mark book unavailable
    ↓
Publish BookBorrowedEvent
    ↓
Kafka consumer processes event
```

---

## Why This Project

This project was designed to demonstrate practical backend engineering concepts beyond simple CRUD:

- REST API design with Spring Web
- Layered architecture and separation of concerns
- Dependency Injection and Spring IoC
- Relational data modeling
- JPA/Hibernate ORM
- Transactional business operations
- Bean validation
- Centralized exception handling
- Asynchronous event-driven processing with Kafka
- Containerized local development with Docker Compose
- Application testing with H2 and Embedded Kafka

The project intentionally combines a conventional request/response backend with an event-driven side path so that synchronous business operations and asynchronous processing can coexist.

---

## Key Features

### Book Management

- Create a book
- List all books
- Fetch a book by ID
- Update a book
- Delete a book
- Track current availability

### Member Management

- Register a member
- List all members

### Borrow / Return Workflow

- Borrow a specific book for a member
- Validate that both book and member exist
- Reject borrowing when a book is unavailable
- Create a persistent `BorrowRecord`
- Mark the borrowed book as unavailable
- Return a previously borrowed book
- Set the return timestamp
- Make the returned book available again

### Event-Driven Processing

- Publish a `BookBorrowedEvent` after a successful borrow
- Send the event to the `book-events` Kafka topic
- Consume the event asynchronously
- Log a notification from the consumer

### Validation and Error Handling

- Bean validation using `@Valid` and validation annotations
- Custom domain exceptions
- Centralized `@RestControllerAdvice`
- Consistent JSON error responses
- Correct HTTP status codes for common failure cases

### Containerization

Docker Compose runs:

- Spring Boot application
- PostgreSQL
- Kafka
- ZooKeeper

---

## Architecture

### High-Level Application Architecture

```text
                           +----------------------+
                           |   Client / Postman   |
                           +----------+-----------+
                                      |
                                   HTTP/JSON
                                      |
                                      v
                           +----------------------+
                           |      Controllers     |
                           | Book / Member /      |
                           | Borrow               |
                           +----------+-----------+
                                      |
                                      v
                           +----------------------+
                           |       Services      |
                           | Book / Member /      |
                           | Borrow               |
                           +----------+-----------+
                                      |
                     +----------------+----------------+
                     |                                 |
                     v                                 v
             +---------------+                  +--------------+
             | Spring Data   |                  | Kafka        |
             | JPA Repos     |                  | Producer     |
             +-------+-------+                  +------+-------+
                     |                                 |
                     v                                 v
             +---------------+                  +--------------+
             | PostgreSQL    |                  | book-events  |
             +---------------+                  +------+-------+
                                                       |
                                                       v
                                                +--------------+
                                                | Kafka        |
                                                | Consumer     |
                                                +--------------+
```

### Why the layers exist

| Layer | Responsibility |
|---|---|
| Controller | Accept HTTP requests, validate request payloads, return HTTP responses |
| Service | Own business rules and transactional workflows |
| Repository | Abstract database persistence |
| Entity | Represent persistent domain objects |
| DTO | Define request payloads and validation boundaries |
| Kafka Producer | Publish domain events |
| Kafka Consumer | React asynchronously to events |
| Exception Handler | Convert exceptions into consistent API errors |
| Config | Define infrastructure/application configuration |

---

## End-to-End Borrow Workflow

Consider:

```text
Book:
ID = 1
Title = Clean Code
Available = true

Member:
ID = 1
Name = Jane Doe
```

The client sends:

```http
POST /borrow/1/1
```

### Step 1 — Request reaches the controller

`BorrowController` receives:

```text
bookId   = 1
memberId = 1
```

and delegates the operation to the service layer.

### Step 2 — Service loads the book

`BorrowService` uses `BookRepository` to find the requested book.

If the book does not exist:

```text
BookNotFoundException
        ↓
GlobalExceptionHandler
        ↓
404 Not Found
```

### Step 3 — Service loads the member

The member is loaded through `MemberRepository`.

If the member does not exist:

```text
MemberNotFoundException
        ↓
GlobalExceptionHandler
        ↓
404 Not Found
```

### Step 4 — Availability is checked

If:

```text
book.available == false
```

the operation is rejected with `BookNotAvailableException`.

### Step 5 — Borrow record is created

A `BorrowRecord` is created with:

```text
book
member
borrowDate = current timestamp
returnDate = null
```

`returnDate = null` means the book is still out on loan.

### Step 6 — Database state is updated

The application:

1. Saves the `BorrowRecord`
2. Changes the book's `available` flag to `false`
3. Saves the updated book

### Step 7 — Kafka event is published

A `BookBorrowedEvent` containing the borrow details is published to:

```text
book-events
```

### Step 8 — Consumer receives the event

The Kafka consumer asynchronously receives the event and currently logs:

```text
Notification: Book 'Clean Code' borrowed by Member 'Jane Doe'
```

### Result

The client receives the created borrow record while the asynchronous Kafka path handles the event separately.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Primary programming language/runtime |
| **Spring Boot 3.3.x** | Application framework |
| **Spring Web** | REST APIs / HTTP layer |
| **Spring Data JPA** | Persistence abstraction |
| **Hibernate** | ORM implementation behind JPA |
| **PostgreSQL** | Relational persistent database |
| **Spring Kafka** | Kafka producer/consumer integration |
| **Apache Kafka** | Event streaming / asynchronous messaging |
| **ZooKeeper** | Coordination for the project's Kafka Docker setup |
| **Maven** | Build and dependency management |
| **Lombok** | Reduces boilerplate such as getters/setters/builders |
| **Docker** | Application/infrastructure containerization |
| **Docker Compose** | Multi-container local environment |
| **H2** | In-memory test database |
| **Embedded Kafka** | Kafka broker for application tests |

---

## Project Structure

```text
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
    │   │   ├── controller/
    │   │   │   ├── BookController.java
    │   │   │   ├── MemberController.java
    │   │   │   └── BorrowController.java
    │   │   ├── service/
    │   │   │   ├── BookService.java
    │   │   │   ├── MemberService.java
    │   │   │   └── BorrowService.java
    │   │   ├── repository/
    │   │   │   ├── BookRepository.java
    │   │   │   ├── MemberRepository.java
    │   │   │   └── BorrowRecordRepository.java
    │   │   ├── entity/
    │   │   │   ├── Book.java
    │   │   │   ├── Member.java
    │   │   │   └── BorrowRecord.java
    │   │   ├── dto/
    │   │   │   ├── BookRequest.java
    │   │   │   └── MemberRequest.java
    │   │   ├── kafka/
    │   │   │   ├── BookBorrowedEvent.java
    │   │   │   ├── BookEventProducer.java
    │   │   │   └── BookEventConsumer.java
    │   │   ├── exception/
    │   │   │   ├── BookNotFoundException.java
    │   │   │   ├── MemberNotFoundException.java
    │   │   │   ├── BorrowRecordNotFoundException.java
    │   │   │   ├── BookNotAvailableException.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   └── config/
    │   │       └── KafkaTopicConfig.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/library/lms/
            └── LibraryManagementSystemApplicationTests.java
```

---

## Domain Model

### Book

```text
Book
-------------------------
id
title
author
isbn
available
```

The `available` flag represents the current state of a physical/catalog item.

### Member

```text
Member
-------------------------
id
name
email
```

### BorrowRecord

```text
BorrowRecord
-------------------------
id
book      -> Book
member    -> Member
borrowDate
returnDate
```

### Relationship

```text
Book 1 -------- * BorrowRecord * -------- 1 Member
```

A book can appear in many historical borrow records over time, and a member can have many borrow records over time.

The current repository models `BorrowRecord.book` and `BorrowRecord.member` as `@ManyToOne` relationships.

---

## API Reference

Base URL:

```text
http://localhost:8080
```

### Books

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/books` | Create a book |
| `GET` | `/books` | List books |
| `GET` | `/books/{id}` | Get a book |
| `PUT` | `/books/{id}` | Update a book |
| `DELETE` | `/books/{id}` | Delete a book |

### Members

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/members` | Register a member |
| `GET` | `/members` | List members |

### Borrow / Return

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/borrow/{bookId}/{memberId}` | Borrow a book |
| `POST` | `/return/{recordId}` | Return a book |

---

## API Examples

### Create Book

```http
POST /books
Content-Type: application/json
```

```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884"
}
```

Example response:

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "available": true
}
```

---

### Register Member

```http
POST /members
Content-Type: application/json
```

```json
{
  "name": "Jane Doe",
  "email": "jane.doe@example.com"
}
```

---

### Borrow Book

```http
POST /borrow/1/1
```

Example response:

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

```text
Book.available
true → false

BorrowRecord
created

Kafka
BookBorrowedEvent published
```

---

### Return Book

```http
POST /return/1
```

Example outcome:

```text
BorrowRecord.returnDate = current timestamp
Book.available = true
```

---

## Kafka Event Flow

### Producer

The borrow service creates a `BookBorrowedEvent` containing details such as:

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

The producer publishes it to:

```text
book-events
```

### Consumer

The consumer subscribes to the same topic using the configured consumer group.

```text
BorrowService
     |
     | publish
     v
+-------------+
| book-events |
+-------------+
     |
     | consume
     v
BookEventConsumer
     |
     v
Log notification
```

### Why Kafka?

Kafka introduces a decoupled asynchronous path.

The core borrow operation does not need to directly know about future consumers such as:

- Notification services
- Analytics services
- Audit services
- Recommendation services
- Activity feeds

The application can publish an event once, while multiple downstream consumers can react to it independently.

---

## Validation and Error Handling

The API uses DTO-based validation with `@Valid`.

Typical validation flow:

```text
HTTP request
    ↓
DTO validation
    ↓
Valid request?
  /     \
No      Yes
 |        |
400       Service
```

Custom exceptions are handled centrally using `@RestControllerAdvice`.

### Common error mapping

| Error | HTTP status | Meaning |
|---|---:|---|
| `BookNotFoundException` | `404` | Book ID does not exist |
| `MemberNotFoundException` | `404` | Member ID does not exist |
| `BorrowRecordNotFoundException` | `404` | Borrow record does not exist |
| `BookNotAvailableException` | `400` | Attempt to borrow an unavailable book |
| Validation errors | `400` | Invalid request payload |
| Unhandled exception | `500` | Unexpected server error |

Example:

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

This keeps API errors predictable for consumers.

---

## Running with Docker Compose

### Prerequisites

Install:

- Docker
- Docker Compose

### Start the entire stack

From the project root:

```bash
docker-compose up --build
```

This starts:

```text
Spring Boot application
PostgreSQL
ZooKeeper
Kafka
```

The application is available at:

```text
http://localhost:8080
```

### Stop the stack

```bash
docker-compose down
```

### Stop and remove PostgreSQL volume

```bash
docker-compose down -v
```

### Follow application logs

```bash
docker-compose logs -f app
```

This is also useful for observing the Kafka consumer processing borrow events.

---

## Running Locally Without Docker

You need:

- Java 21
- Maven
- PostgreSQL
- Kafka
- ZooKeeper

Set environment variables when needed:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=library_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Then run:

```bash
./mvnw spring-boot:run
```

The project also provides sensible localhost defaults through `application.yml`.

---

## Example API Flow

A simple manual test sequence:

### 1. Create a book

```bash
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884"
  }'
```

### 2. Create a member

```bash
curl -X POST http://localhost:8080/members \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  }'
```

### 3. Borrow the book

```bash
curl -X POST http://localhost:8080/borrow/1/1
```

### 4. Return the book

```bash
curl -X POST http://localhost:8080/return/1
```

### Expected state transition

```text
Book.available

true
  |
  | borrow
  v
false
  |
  | return
  v
true
```

---

## Testing

The repository includes a Spring Boot test setup using:

- H2 in-memory database
- Embedded Kafka

The existing test verifies that the application context can start successfully with the test infrastructure.

This is useful for validating wiring/configuration without requiring a separately running PostgreSQL server or Kafka broker.

### Suggested test coverage for production

The next level of testing would include:

- Book CRUD tests
- Member registration tests
- Borrow success/failure tests
- Return workflow tests
- Validation tests
- Exception-handler tests
- Kafka producer/consumer integration tests
- Concurrency tests for simultaneous borrow requests

---

## Design Decisions

### Why layered architecture?

It separates responsibilities:

```text
Controller → HTTP concerns
Service    → business rules
Repository → persistence
```

This improves maintainability and makes individual components easier to test.

### Why DTOs?

DTOs isolate API contracts from persistence entities and provide a clean place for request validation.

### Why PostgreSQL?

The domain is strongly relational:

```text
Book
Member
BorrowRecord
```

with explicit relationships between borrow records, books, and members.

### Why JPA/Hibernate?

JPA/Hibernate reduces repetitive SQL and maps Java domain objects to relational tables.

### Why Kafka?

The project uses Kafka to demonstrate asynchronous event-driven processing and reduce coupling between the core borrowing workflow and downstream consumers.

### Why Docker Compose?

Running the entire local stack with one command improves developer setup consistency and removes the need for manually configuring every dependency.

---

## Concurrency and Reliability Considerations

The current implementation uses a service-level availability check:

```text
if book is unavailable
    reject
else
    create borrow record
    mark unavailable
```

For a single-user or low-contention environment, this is straightforward. In a highly concurrent production system, two requests could race between reading and updating the same book.

### Production approaches

Possible solutions include:

#### 1. Pessimistic locking

Lock the book row while performing the borrow transaction.

```sql
SELECT *
FROM books
WHERE id = ?
FOR UPDATE;
```

#### 2. Optimistic locking

Add a version field:

```java
@Version
private Long version;
```

and reject conflicting updates.

#### 3. Atomic conditional update

Perform the state transition directly in SQL:

```sql
UPDATE books
SET available = false
WHERE id = ?
  AND available = true;
```

Then verify that exactly one row was updated.

---

## Kafka / Database Consistency Consideration

The database transaction and Kafka event publication are separate concerns in the current implementation.

That means a failure could theoretically produce:

```text
Database transaction succeeds
        ↓
Kafka publication fails
        ↓
DB state exists, event is missing
```

For stronger reliability, a production implementation could use an **Outbox Pattern**:

```text
              Single DB transaction
                     |
          +----------+-----------+
          |                      |
          v                      v
   BorrowRecord update      Outbox event
                                  |
                                  v
                         Reliable publisher
                                  |
                                  v
                                Kafka
```

This would make event publication much more resilient to transient messaging failures.

---

## Production Improvements

Potential next steps for evolving this project:

### Security

- Spring Security
- JWT/OAuth2 authentication
- Role-based authorization
- Admin/member permissions

### API Quality

- Pagination
- Sorting
- Search/filtering
- OpenAPI / Swagger documentation
- API versioning

### Data Integrity

- Stronger DB constraints
- Unique member/email handling
- More explicit foreign key constraints
- Optimistic/pessimistic locking where required

### Event Reliability

- Outbox Pattern
- Retry policies
- Dead-letter topics
- Idempotent consumers
- Monitoring and alerting

### Observability

- Structured logging
- Spring Boot Actuator
- Micrometer metrics
- Distributed tracing
- Kafka lag monitoring

### Scalability

- Multiple Kafka partitions
- Multiple consumer instances
- Read/write optimization
- Database indexing
- Caching with Redis where justified

### Deployment

- CI/CD pipeline
- Kubernetes
- Cloud-managed PostgreSQL/Kafka
- Secrets management
- Separate dev/staging/prod configurations

---

## Interview Talking Points

This project can be explained in an interview using the following structure:

### 1. Start with the problem

> "I built a backend library system that manages books, members, and borrow/return transactions."

### 2. Explain the architecture

> "I used a layered Spring Boot architecture with Controllers, Services, Repositories and JPA entities."

### 3. Explain the database

> "PostgreSQL stores books, members and borrow records, with many-to-one relationships from borrow records to books and members."

### 4. Explain the interesting part

> "When a book is successfully borrowed, I publish a `BookBorrowedEvent` to Kafka. A consumer processes the event asynchronously."

### 5. Explain why Kafka

> "The event-driven path decouples the main borrow workflow from downstream actions such as notifications or analytics."

### 6. Explain transaction handling

> "The borrow service is transactional for the database changes, so the borrow record and book availability update are treated as one database unit of work."

### 7. Explain production considerations

> "For higher concurrency I would introduce database-level locking or an atomic update, and for reliable database-to-Kafka delivery I would consider an Outbox Pattern."

This demonstrates both knowledge of the current implementation and awareness of production engineering concerns.

---

## Interview Questions This Project Can Lead To

Be prepared to answer:

- Why Spring Boot?
- What is Dependency Injection?
- What is IoC?
- Why use a Service layer?
- Why use DTOs instead of entities in API requests?
- What is JPA?
- What is Hibernate?
- What is ORM?
- Why PostgreSQL?
- Why `@ManyToOne`?
- What does `@Transactional` do?
- What happens during `repository.save()`?
- What is lazy loading?
- What is a REST API?
- Why use `201 Created` vs `200 OK`?
- What is `@RestControllerAdvice`?
- Why Kafka?
- What is a Kafka topic?
- What is a partition?
- What is a consumer group?
- What happens if Kafka is unavailable?
- Can two users borrow the same book simultaneously?
- How would you prevent double borrowing?
- How would you scale this system?
- How would you make the DB-to-Kafka workflow reliable?
- Why Docker Compose?
- Why use H2 for tests?
- How would you productionize this system?

---

## Future Enhancements

The current system intentionally keeps the domain compact. Possible feature extensions:

- Due dates and overdue tracking
- Fine calculation
- Book search and filtering
- Multiple copies of the same book
- Reservation/waitlist functionality
- Member borrowing limits
- Email/SMS/push notifications
- Admin dashboard
- Authentication and authorization
- Audit history
- Metrics and observability
- Redis caching
- Outbox + retry/dead-letter architecture

---

## Author

**Harshit Singh**

NIT Calicut

- GitHub: [Harshit-0018](https://github.com/Harshit-0018)
- LinkedIn: [Harshit Singh](https://www.linkedin.com/in/harshitsinghnitc/)

---

## License

