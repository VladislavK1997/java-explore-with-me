package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EwmApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethodStartsApplication() {
        MainServiceApplication.main(new String[]{});
    }
}