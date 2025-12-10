package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"ru.practicum.ewm", "ru.practicum.ewm.stat"})
@EntityScan(basePackages = {"ru.practicum.ewm.model"})
@EnableJpaRepositories(basePackages = {"ru.practicum.ewm.repository"})
public class MainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainServiceApplication.class, args);
    }
}