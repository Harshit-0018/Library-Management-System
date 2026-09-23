package com.library.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that verifies the Spring application context loads successfully,
 * using an embedded Kafka broker and an in-memory H2 database so it can run
 * without any external infrastructure.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"book-events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@ActiveProfiles("test")
class LibraryManagementSystemApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context starts successfully, this test passes.
    }

}
