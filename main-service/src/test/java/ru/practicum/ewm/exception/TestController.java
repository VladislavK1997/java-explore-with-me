package ru.practicum.ewm.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/test/missing-param")
    public String testMissingParam(@RequestParam String requiredParam) {
        return "OK";
    }

    @GetMapping("/test/null-pointer")
    public String testNullPointer() {
        throw new NullPointerException("Test NPE");
    }
}