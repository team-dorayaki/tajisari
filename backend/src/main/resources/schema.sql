CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 식별자',
    user_key VARCHAR(36) NOT NULL COMMENT '익명 사용자 키',
    created_at DATETIME(6) NOT NULL COMMENT '생성일시',

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_user_key UNIQUE (user_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE property (
    property_id BIGINT NOT NULL AUTO_INCREMENT,
    source_site VARCHAR(50) NULL COMMENT '예: SUUMO, LEOPALACE21, UR임대',
    source_url VARCHAR(2048) NULL,
    source_url_hash BINARY(32)
        GENERATED ALWAYS AS (UNHEX(SHA2(source_url, 256))) STORED,
    property_name VARCHAR(255) NULL,
    prefecture VARCHAR(50) NULL COMMENT '예: 도쿄도',
    city VARCHAR(100) NULL COMMENT '예: 신주쿠구, 네리마구',
    exclusive_area_m2 DECIMAL(8, 2) NULL COMMENT '전용면적(㎡)',
    nearest_station VARCHAR(100) NULL,
    walk_minutes INT NULL,
    rent BIGINT NULL,
    management_fee BIGINT NULL,
    deposit BIGINT NULL COMMENT '0=없음, NULL=미확인',
    key_money BIGINT NULL COMMENT '0=없음, NULL=미확인',
    available_from DATE NULL COMMENT '날짜 미확정이면 NULL',
    contract_period_months INT NULL,
    listed_initial_cost_total BIGINT NULL,
    confirmed_initial_cost BIGINT NULL,
    confirmed_monthly_cost BIGINT NULL,
    priority_rank TINYINT NULL COMMENT '매물 우선순위: 1, 2 / 미선택 NULL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_property PRIMARY KEY (property_id),
    CONSTRAINT uk_property_source_url UNIQUE (source_url_hash),
    CONSTRAINT chk_property_exclusive_area CHECK (exclusive_area_m2 IS NULL OR exclusive_area_m2 >= 0),
    CONSTRAINT chk_property_walk_minutes CHECK (walk_minutes IS NULL OR walk_minutes >= 0),
    CONSTRAINT chk_property_amounts CHECK (
        (rent IS NULL OR rent >= 0)
        AND (management_fee IS NULL OR management_fee >= 0)
        AND (deposit IS NULL OR deposit >= 0)
        AND (key_money IS NULL OR key_money >= 0)
        AND (listed_initial_cost_total IS NULL OR listed_initial_cost_total >= 0)
        AND (confirmed_initial_cost IS NULL OR confirmed_initial_cost >= 0)
        AND (confirmed_monthly_cost IS NULL OR confirmed_monthly_cost >= 0)
    ),
    CONSTRAINT chk_property_contract_period
        CHECK (contract_period_months IS NULL OR contract_period_months > 0),
    CONSTRAINT chk_property_priority_rank
        CHECK (priority_rank IS NULL OR priority_rank IN (1, 2)),
    CONSTRAINT uk_property_priority_rank UNIQUE (priority_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE property_cost_item (
    cost_item_id BIGINT NOT NULL AUTO_INCREMENT,
    property_id BIGINT NOT NULL,
    raw_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    amount BIGINT NULL COMMENT '미확인 시 NULL',
    raw_value TEXT NULL COMMENT '예: 1ヶ月, 総賃料50%, 2年20,000円',
    obligation_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN'
        COMMENT 'REQUIRED, OPTIONAL, UNKNOWN',
    is_included_in_calculation BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '사용자 선택에 따른 정착비 계산 포함 여부',
    timing VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN'
        COMMENT 'INITIAL, MONTHLY, RENEWAL, MOVE_OUT, CONDITIONAL, UNKNOWN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_property_cost_item PRIMARY KEY (cost_item_id),
    CONSTRAINT fk_property_cost_item_property
        FOREIGN KEY (property_id) REFERENCES property (property_id) ON DELETE CASCADE,
    CONSTRAINT chk_property_cost_item_amount
        CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT chk_property_cost_item_obligation_status
        CHECK (obligation_status IN ('REQUIRED', 'OPTIONAL', 'UNKNOWN')),
    CONSTRAINT chk_property_cost_item_timing
        CHECK (timing IN ('INITIAL', 'MONTHLY', 'RENEWAL', 'MOVE_OUT', 'CONDITIONAL', 'UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE property_ai_analysis (
    analysis_id BIGINT NOT NULL AUTO_INCREMENT,
    property_id BIGINT NOT NULL,
    source_type VARCHAR(20) NOT NULL COMMENT 'IMAGE, URL, BOTH',
    raw_json JSON NOT NULL,
    model_version VARCHAR(100) NULL COMMENT 'AI 모델 또는 프롬프트 버전',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_property_ai_analysis PRIMARY KEY (analysis_id),
    CONSTRAINT fk_property_ai_analysis_property
        FOREIGN KEY (property_id) REFERENCES property (property_id) ON DELETE CASCADE,
    CONSTRAINT chk_property_ai_analysis_source_type
        CHECK (source_type IN ('IMAGE', 'URL', 'BOTH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE property_image (
    property_image_id BIGINT NOT NULL AUTO_INCREMENT,
    property_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    image_order INT NOT NULL,
    original_filename VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_property_image PRIMARY KEY (property_image_id),
    CONSTRAINT fk_property_image_property
        FOREIGN KEY (property_id) REFERENCES property (property_id) ON DELETE CASCADE,
    CONSTRAINT uk_property_image_order UNIQUE (property_id, image_order),
    CONSTRAINT chk_property_image_order CHECK (image_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE settlement_plans (
    settlement_plan_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '정착 계획 식별자',
    user_id BIGINT NOT NULL COMMENT '익명 사용자 ID',
    move_in_date DATE NOT NULL COMMENT '입주 예정일',
    planned_stay_months INT NOT NULL COMMENT '예상 체류기간(개월)',
    prepared_funds_krw BIGINT NOT NULL COMMENT '원화 준비자금',
    prepared_funds_jpy BIGINT NOT NULL COMMENT '엔화 준비자금',
    emergency_reserve_krw BIGINT NOT NULL COMMENT '원화 비상예비비',
    emergency_reserve_jpy BIGINT NOT NULL COMMENT '엔화 비상예비비',
    monthly_living_cost_input_method VARCHAR(16) NOT NULL COMMENT '월 생활비 입력 방식',
    created_at DATETIME(6) NOT NULL COMMENT '생성일시',
    updated_at DATETIME(6) NOT NULL COMMENT '수정일시',

    CONSTRAINT pk_settlement_plans PRIMARY KEY (settlement_plan_id),
    CONSTRAINT uk_settlement_plans_user UNIQUE (user_id),
    CONSTRAINT fk_settlement_plans_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_settlement_plans_stay_months
        CHECK (planned_stay_months BETWEEN 1 AND 24),
    CONSTRAINT chk_settlement_plans_prepared_funds
        CHECK (prepared_funds_krw >= 0 AND prepared_funds_jpy >= 0),
    CONSTRAINT chk_settlement_plans_emergency_reserve
        CHECK (
            emergency_reserve_krw BETWEEN 0 AND prepared_funds_krw
            AND emergency_reserve_jpy BETWEEN 0 AND prepared_funds_jpy
        ),
    CONSTRAINT chk_settlement_plans_monthly_living_cost_input_method
        CHECK (monthly_living_cost_input_method IN ('DIRECT', 'DEFAULT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE settlement_plan_cost_items (
    settlement_plan_cost_item_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '정착 계획 비용 항목 식별자',
    settlement_plan_id BIGINT NOT NULL COMMENT '정착 계획 ID',
    cost_category VARCHAR(16) NOT NULL COMMENT '비용 구분',
    cost_type VARCHAR(32) NOT NULL COMMENT '비용 종류',
    amount BIGINT NOT NULL COMMENT '비용 금액',
    currency VARCHAR(3) NOT NULL COMMENT '비용 통화',

    CONSTRAINT pk_settlement_plan_cost_items
        PRIMARY KEY (settlement_plan_cost_item_id),
    CONSTRAINT uk_cost_items_plan_category_type
        UNIQUE (settlement_plan_id, cost_category, cost_type),
    CONSTRAINT fk_cost_items_settlement_plan
        FOREIGN KEY (settlement_plan_id)
        REFERENCES settlement_plans (settlement_plan_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_cost_items_category
        CHECK (cost_category IN ('INITIAL', 'MONTHLY')),
    CONSTRAINT chk_cost_items_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_cost_items_currency
        CHECK (currency IN ('KRW', 'JPY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
