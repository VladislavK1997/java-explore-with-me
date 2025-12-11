package ru.practicum.ewm.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category category1;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .build();
        user2 = userRepository.save(user2);

        category1 = Category.builder()
                .name("Concerts")
                .build();
        category1 = categoryRepository.save(category1);
    }

    @Test
    void userRepository_saveAndFindById_shouldWork() {
        User newUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        Optional<User> foundUserOpt = userRepository.findById(savedUser.getId());
        assertTrue(foundUserOpt.isPresent());

        User foundUser = foundUserOpt.get();
        assertEquals("Test User", foundUser.getName());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void userRepository_findByIdIn_shouldReturnFilteredUsers() {
        List<User> users = userRepository.findByIdIn(
                List.of(user1.getId(), user2.getId()),
                PageRequest.of(0, 10));

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("john@example.com")));
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("jane@example.com")));
    }

    @Test
    void userRepository_findByIdIn_withNonExistingIds_shouldReturnEmptyList() {
        List<User> users = userRepository.findByIdIn(
                List.of(999L, 1000L),
                PageRequest.of(0, 10));

        assertTrue(users.isEmpty());
    }

    @Test
    void categoryRepository_saveAndFindById_shouldWork() {
        Category newCategory = Category.builder()
                .name("Cinema")
                .build();

        Category savedCategory = categoryRepository.save(newCategory);
        entityManager.flush();

        Category foundCategory = categoryRepository.findById(savedCategory.getId()).orElse(null);

        assertNotNull(foundCategory);
        assertEquals("Cinema", foundCategory.getName());
    }
}