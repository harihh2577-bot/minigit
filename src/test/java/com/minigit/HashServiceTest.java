package com.minigit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.minigit.core.HashService;
import org.junit.jupiter.api.Test;

public class HashServiceTest {

    @Test
    void hashOfHelloIsKnownValue() {
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        assertEquals(expected, HashService.sha256("hello"));
    }

    @Test
    void hashOfEmptyStringIsKnownValue() {
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertEquals(expected, HashService.sha256(""));
    }

    @Test
    void sameInputGivesSameHash() {
        assertEquals(HashService.sha256("abc"), HashService.sha256("abc"));
    }

    @Test
    void differentInputGivesDifferentHash() {
        assertNotEquals(HashService.sha256("hello"), HashService.sha256("hello!"));
    }
}