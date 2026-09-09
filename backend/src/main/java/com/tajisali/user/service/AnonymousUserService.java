package com.tajisali.user.service;

import com.tajisali.user.domain.User;
import com.tajisali.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousUserService {

    private final UserRepository userRepository;

    public User resolveOrCreate(String userKey) {
        return findExistingUser(userKey)
                .orElseGet(() -> userRepository.save(new User(UUID.randomUUID().toString())));
    }

    private Optional<User> findExistingUser(String userKey) {
        if (!isUuid(userKey)) {
            return Optional.empty();
        }
        return userRepository.findByUserKey(userKey);
    }

    private boolean isUuid(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
