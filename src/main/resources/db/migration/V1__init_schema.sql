CREATE TABLE users
(
    user_id      BIGSERIAL    NOT NULL,
    email        VARCHAR(50)  NOT NULL,
    password     VARCHAR(255) NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    subscription BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id),
    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE skin_type
(
    skin_type_id BIGSERIAL   NOT NULL,
    type_name    VARCHAR(50) NOT NULL,
    PRIMARY KEY (skin_type_id)
);

CREATE TABLE skin_concern
(
    skin_concern_id BIGSERIAL   NOT NULL,
    concern_name    VARCHAR(50) NOT NULL,
    PRIMARY KEY (skin_concern_id)
);

CREATE TABLE user_skin_type
(
    user_skin_type_id BIGSERIAL NOT NULL,
    user_id           BIGINT    NOT NULL,
    skin_type_id      BIGINT    NOT NULL,
    PRIMARY KEY (user_skin_type_id),
    CONSTRAINT uq_user_skin_type UNIQUE (user_id, skin_type_id),
    CONSTRAINT fk_ust_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ust_skin_type FOREIGN KEY (skin_type_id) REFERENCES skin_type (skin_type_id)
);

CREATE TABLE user_skin_concern
(
    user_skin_concern_id BIGSERIAL NOT NULL,
    user_id              BIGINT    NOT NULL,
    skin_concern_id      BIGINT    NOT NULL,
    PRIMARY KEY (user_skin_concern_id),
    CONSTRAINT uq_user_skin_concern UNIQUE (user_id, skin_concern_id),
    CONSTRAINT fk_usc_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_usc_skin_concern FOREIGN KEY (skin_concern_id) REFERENCES skin_concern (skin_concern_id)
);

CREATE TABLE menstruation_cycle
(
    menstruation_cycle_id BIGSERIAL NOT NULL,
    user_id               BIGINT    NOT NULL,
    cycle_start_date      DATE      NULL,
    cycle_end_date        DATE      NULL,
    period_duration       INT       NULL DEFAULT 4,
    predicted_cycle_length INT      NULL DEFAULT 28,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (menstruation_cycle_id),
    CONSTRAINT fk_mc_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE cycle_phase
(
    cycle_phase_id        BIGSERIAL    NOT NULL,
    menstruation_cycle_id BIGINT       NOT NULL,
    start_date            DATE         NOT NULL,
    end_date              DATE         NOT NULL,
    phase_type            VARCHAR(20)  NOT NULL,
    PRIMARY KEY (cycle_phase_id),
    CONSTRAINT chk_cycle_phase_type CHECK (phase_type IN ('MENSTRUATION', 'FOLLICULAR', 'OVULATION', 'LUTEAL')),
    CONSTRAINT fk_cp_cycle FOREIGN KEY (menstruation_cycle_id) REFERENCES menstruation_cycle (menstruation_cycle_id)
);

CREATE TABLE today_skin
(
    today_skin_id BIGSERIAL    NOT NULL,
    user_id       BIGINT       NOT NULL,
    log_date      DATE         NOT NULL,
    image_url     VARCHAR(255) NULL,
    sleep_time    INT          NULL,
    water_intake  INT          NULL,
    diet_type     VARCHAR(20)  NULL,
    exercise_time VARCHAR(20)  NULL,
    skin_status   VARCHAR(20)  NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (today_skin_id),
    CONSTRAINT uq_today_skin_user_date UNIQUE (user_id, log_date),
    CONSTRAINT chk_diet_type CHECK (diet_type IN ('VEGETABLE_FOCUSED', 'FAST_FOOD', 'SPICY_FOOD', 'BALANCED', 'ALCOHOL')),
    CONSTRAINT chk_exercise_time CHECK (exercise_time IN ('ZERO_M', 'THIRTY_M', 'SIXTY_M', 'NINETY_M_PLUS')),
    CONSTRAINT chk_skin_status CHECK (skin_status IN ('DRY', 'OILY', 'SENSITIVE', 'GOOD')),
    CONSTRAINT fk_ts_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE ai_analysis
(
    analysis_id   BIGSERIAL NOT NULL,
    today_skin_id BIGINT    NOT NULL,
    overall_score INT       NULL,
    ai_comment    TEXT      NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (analysis_id),
    CONSTRAINT uq_analysis_today_skin UNIQUE (today_skin_id),
    CONSTRAINT fk_aa_today_skin FOREIGN KEY (today_skin_id) REFERENCES today_skin (today_skin_id)
);

CREATE TABLE detailed_skin_analysis
(
    detail_analysis_id BIGSERIAL NOT NULL,
    analysis_id        BIGINT    NOT NULL,
    sebum_level        INT       NULL,
    redness            INT       NULL,
    hydration_level    INT       NULL,
    pore_condition     INT       NULL,
    skin_elasticity    INT       NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (detail_analysis_id),
    CONSTRAINT uq_detail_analysis UNIQUE (analysis_id),
    CONSTRAINT fk_dsa_analysis FOREIGN KEY (analysis_id) REFERENCES ai_analysis (analysis_id)
);

CREATE TABLE ai_daily_routine
(
    routine_id       BIGSERIAL    NOT NULL,
    user_id          BIGINT       NOT NULL,
    analysis_id      BIGINT       NOT NULL,
    target_date      DATE         NOT NULL,
    phase_type       VARCHAR(20)  NOT NULL,
    skincare_routine TEXT         NOT NULL,
    action_content   TEXT         NOT NULL,
    exercise_routine TEXT         NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (routine_id),
    CONSTRAINT chk_routine_phase_type CHECK (phase_type IN ('MENSTRUATION', 'FOLLICULAR', 'OVULATION', 'LUTEAL')),
    CONSTRAINT fk_adr_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_adr_analysis FOREIGN KEY (analysis_id) REFERENCES ai_analysis (analysis_id)
);

CREATE TABLE product
(
    product_id BIGSERIAL    NOT NULL,
    prod_name  VARCHAR(100) NULL,
    purpose    VARCHAR(50)  NULL,
    price      INT          NULL,
    image_url  VARCHAR(255) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id)
);

CREATE TABLE prod_recommend
(
    prod_recommend_id BIGSERIAL NOT NULL,
    analysis_id       BIGINT    NOT NULL,
    product_id        BIGINT    NOT NULL,
    match_percentage  INT       NULL,
    PRIMARY KEY (prod_recommend_id),
    CONSTRAINT fk_pr_analysis FOREIGN KEY (analysis_id) REFERENCES ai_analysis (analysis_id),
    CONSTRAINT fk_pr_product FOREIGN KEY (product_id) REFERENCES product (product_id)
);

CREATE TABLE ai_chat_room
(
    chat_room_id BIGSERIAL    NOT NULL,
    user_id      BIGINT       NOT NULL,
    analysis_id  BIGINT       NULL,
    title        VARCHAR(100) NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (chat_room_id),
    CONSTRAINT fk_acr_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_acr_analysis FOREIGN KEY (analysis_id) REFERENCES ai_analysis (analysis_id)
);

CREATE TABLE ai_chat_message
(
    chat_message_id BIGSERIAL    NOT NULL,
    chat_room_id    BIGINT       NOT NULL,
    role            VARCHAR(10)  NOT NULL,
    message_type    VARCHAR(10)  NOT NULL DEFAULT 'TEXT',
    content         TEXT         NULL,
    file_url        VARCHAR(255) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (chat_message_id),
    CONSTRAINT chk_message_role CHECK (role IN ('AI', 'USER')),
    CONSTRAINT chk_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')),
    CONSTRAINT fk_acm_chat_room FOREIGN KEY (chat_room_id) REFERENCES ai_chat_room (chat_room_id)
);
