package com.tajisali.user.repository;

import com.tajisali.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 사용자_키와_생성일시가_저장된다() {
        String userKey = UUID.randomUUID().toString();
        User savedUser = userRepository.saveAndFlush(new User(userKey));
        Long userId = savedUser.getId();
        entityManager.clear();

        User foundUser = userRepository.findById(userId).orElseThrow();

        assertThat(foundUser.getUserKey()).isEqualTo(userKey);
        assertThat(foundUser.getCreatedAt()).isNotNull();
    }

    @Test
    void 같은_사용자_키는_중복_저장할_수_없다() {
        String userKey = UUID.randomUUID().toString();
        userRepository.saveAndFlush(new User(userKey));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User(userKey)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
