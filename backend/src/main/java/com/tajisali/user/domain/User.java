package com.tajisali.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_user_key",
                columnNames = "user_key"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", comment = "사용자 식별자")
    private Long id;

    @Column(name = "user_key", nullable = false, length = 36, comment = "익명 사용자 키")
    private String userKey;

    @Column(
            name = "has_compared_properties",
            nullable = false,
            columnDefinition = "BOOLEAN",
            comment = "매물 비교 완료 이력 여부"
    )
    private boolean hasComparedProperties;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)", comment = "생성일시")
    private LocalDateTime createdAt;

    public User(String userKey) {
        this.userKey = userKey;
    }

    public void markPropertiesCompared() {
        hasComparedProperties = true;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(KOREA_ZONE_ID);
    }
}
