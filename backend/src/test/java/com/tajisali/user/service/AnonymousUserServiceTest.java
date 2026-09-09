package com.tajisali.user.service;

import com.tajisali.user.domain.User;
import com.tajisali.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnonymousUserServiceTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000501";

    @Mock
    private UserRepository userRepository;

    private AnonymousUserService anonymousUserService;

    @BeforeEach
    void setUp() {
        anonymousUserService = new AnonymousUserService(userRepository);
    }

    @Test
    void 유효한_사용자_키는_기존_사용자를_재사용한다() {
        User existingUser = new User(USER_KEY);
        when(userRepository.findByUserKey(USER_KEY)).thenReturn(Optional.of(existingUser));

        User resolvedUser = anonymousUserService.resolveOrCreate(USER_KEY);

        assertThat(resolvedUser).isSameAs(existingUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_사용자_키는_UUID_키로_새_사용자를_생성한다() {
        when(userRepository.findByUserKey(USER_KEY)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User resolvedUser = anonymousUserService.resolveOrCreate(USER_KEY);

        assertThat(UUID.fromString(resolvedUser.getUserKey()).toString())
                .isEqualTo(resolvedUser.getUserKey());
        verify(userRepository).save(resolvedUser);
    }

    @Test
    void 사용자_키가_없으면_조회하지_않고_새_사용자를_하나만_생성한다() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User resolvedUser = anonymousUserService.resolveOrCreate(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).isSameAs(resolvedUser);
        assertThat(UUID.fromString(resolvedUser.getUserKey())).isNotNull();
        verify(userRepository, never()).findByUserKey(any());
    }

    @Test
    void 잘못된_형식의_사용자_키는_조회하지_않고_새_사용자를_생성한다() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User resolvedUser = anonymousUserService.resolveOrCreate("not-a-uuid");

        assertThat(UUID.fromString(resolvedUser.getUserKey())).isNotNull();
        verify(userRepository, never()).findByUserKey(any());
        verify(userRepository).save(resolvedUser);
    }

    @Test
    void 읽기_조회는_유효한_사용자_키의_기존_사용자를_반환하고_저장하지_않는다() {
        User existingUser = new User(USER_KEY);
        when(userRepository.findByUserKey(USER_KEY)).thenReturn(Optional.of(existingUser));

        Optional<User> foundUser = anonymousUserService.findExisting(USER_KEY);

        assertThat(foundUser).containsSame(existingUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 읽기_조회는_stale_사용자_키에서_새_사용자를_만들지_않는다() {
        when(userRepository.findByUserKey(USER_KEY)).thenReturn(Optional.empty());

        Optional<User> foundUser = anonymousUserService.findExisting(USER_KEY);

        assertThat(foundUser).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "not-a-uuid")
    void 읽기_조회는_누락되거나_잘못된_키에서_Repository를_호출하지_않는다(String userKey) {
        Optional<User> foundUser = anonymousUserService.findExisting(userKey);

        assertThat(foundUser).isEmpty();
        verifyNoInteractions(userRepository);
    }
}
