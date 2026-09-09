package com.tajisali.property.repository;

import com.tajisali.property.domain.Property;
import com.tajisali.user.domain.User;
import com.tajisali.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PropertyRepositoryTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 사용자별로_같은_URL과_우선순위를_저장하고_각자_매물만_조회한다() {
        User firstUser = saveUser();
        User secondUser = saveUser();

        Property firstProperty = propertyRepository.saveAndFlush(
                property(firstUser, "https://suumo.jp/chintai/example", 1));
        Property secondProperty = propertyRepository.saveAndFlush(
                property(secondUser, "https://suumo.jp/chintai/example", 1));
        entityManager.clear();

        assertThat(propertyRepository.findAllByUserIdOrderByCreatedAtDesc(firstUser.getId()))
                .extracting(Property::getId)
                .containsExactly(firstProperty.getId());
        assertThat(propertyRepository.findAllByUserIdOrderByCreatedAtDesc(secondUser.getId()))
                .extracting(Property::getId)
                .containsExactly(secondProperty.getId());
        assertThat(propertyRepository.findByIdAndUserId(firstProperty.getId(), secondUser.getId()))
                .isEmpty();
    }

    private User saveUser() {
        return userRepository.saveAndFlush(new User(UUID.randomUUID().toString()));
    }

    private Property property(User user, String sourceUrl, int priorityRank) {
        Property property = new Property(
                "사용자별 매물", 70_000L, null, null, priorityRank, LocalDateTime.now());
        property.assignOwner(user);
        ReflectionTestUtils.setField(property, "sourceUrl", sourceUrl);
        return property;
    }
}
