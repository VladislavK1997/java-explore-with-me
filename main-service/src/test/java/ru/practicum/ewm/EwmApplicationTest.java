package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EwmApplicationTest {

    @Test
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    void mainMethodStartsApplication() {
        MainServiceApplication.main(new String[]{});
    }
}