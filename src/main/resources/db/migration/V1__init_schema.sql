CREATE TABLE `users`
(
    `user_id`      BIGINT       NOT NULL AUTO_INCREMENT,
    `email`        VARCHAR(50)  NOT NULL,
    `password`     VARCHAR(255) NOT NULL,
    `name`         VARCHAR(50)  NOT NULL,
    `subscription` BOOLEAN      NOT NULL DEFAULT FALSE,
    `created_at`   DATETIME     NOT NULL DEFAULT NOW(),
    `updated_at`   DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uq_user_email` (`email`)
);

CREATE TABLE `skin_type`
(
    `skin_type_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `type_name`    VARCHAR(50) NOT NULL COMMENT 'OILY, DRY, COMBINATION, SENSITIVE, WEAK_ACIDIC, TROUBLE 등',
    PRIMARY KEY (`skin_type_id`)
);

CREATE TABLE `skin_concern`
(
    `skin_concern_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `concern_name`    VARCHAR(50) NOT NULL COMMENT 'ACNE, REDNESS, DRYNESS, BLACKHEAD, DULLNESS, BLEMISH, ELASTICITY, WRINKLE 등',
    PRIMARY KEY (`skin_concern_id`)
);

CREATE TABLE `user_skin_type`
(
    `user_skin_type_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT NOT NULL,
    `skin_type_id`      BIGINT NOT NULL,
    PRIMARY KEY (`user_skin_type_id`),
    UNIQUE KEY `uq_user_skin_type` (`user_id`, `skin_type_id`),
    CONSTRAINT `fk_ust_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_ust_skin_type` FOREIGN KEY (`skin_type_id`) REFERENCES `skin_type` (`skin_type_id`)
);

CREATE TABLE `user_skin_concern`
(
    `user_skin_concern_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`              BIGINT NOT NULL,
    `skin_concern_id`      BIGINT NOT NULL,
    PRIMARY KEY (`user_skin_concern_id`),
    UNIQUE KEY `uq_user_skin_concern` (`user_id`, `skin_concern_id`),
    CONSTRAINT `fk_usc_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_usc_skin_concern` FOREIGN KEY (`skin_concern_id`) REFERENCES `skin_concern` (`skin_concern_id`)
);

CREATE TABLE `menstruation_cycle`
(
    `menstruation_cycle_id`  BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`                BIGINT   NOT NULL,
    `cycle_start_date`       DATE     NULL     COMMENT '사용자가 직접 기록하는 실제 시작일',
    `cycle_end_date`         DATE     NULL     COMMENT '사용자가 직접 기록하는 실제 종료일',
    `period_duration`        INT      NULL     DEFAULT 4  COMMENT '생리 지속 기간 (예: 4일, 5일)',
    `predicted_cycle_length` INT      NULL     DEFAULT 28 COMMENT '예상되는 다음 생리 주기 일수',
    `created_at`             DATETIME NOT NULL DEFAULT NOW(),
    `updated_at`             DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`menstruation_cycle_id`),
    CONSTRAINT `fk_mc_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `cycle_phase`
(
    `cycle_phase_id`        BIGINT                                                     NOT NULL AUTO_INCREMENT,
    `menstruation_cycle_id` BIGINT                                                     NOT NULL,
    `start_date`            DATE                                                       NOT NULL,
    `end_date`              DATE                                                       NOT NULL,
    `phase_type`            ENUM ('MENSTRUATION', 'FOLLICULAR', 'OVULATION', 'LUTEAL') NOT NULL COMMENT '생리기간, 난포기, 배란기, 황체기',
    PRIMARY KEY (`cycle_phase_id`),
    CONSTRAINT `fk_cp_cycle` FOREIGN KEY (`menstruation_cycle_id`) REFERENCES `menstruation_cycle` (`menstruation_cycle_id`)
);

CREATE TABLE `today_skin`
(
    `today_skin_id` BIGINT                                                                        NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT                                                                        NOT NULL,
    `log_date`      DATE                                                                          NOT NULL COMMENT '기록 날짜',
    `image_url`     VARCHAR(255)                                                                  NULL     COMMENT '피부사진',
    `sleep_time`    INT                                                                           NULL,
    `water_intake`  INT                                                                           NULL,
    `diet_type`     ENUM ('VEGETABLE_FOCUSED', 'FAST_FOOD', 'SPICY_FOOD', 'BALANCED', 'ALCOHOL') NULL,
    `exercise_time` ENUM ('ZERO_M', 'THIRTY_M', 'SIXTY_M', 'NINETY_M_PLUS')                       NULL,
    `skin_status`   ENUM ('DRY', 'OILY', 'SENSITIVE', 'GOOD')                                    NULL,
    `created_at`    DATETIME                                                                      NOT NULL DEFAULT NOW(),
    `updated_at`    DATETIME                                                                      NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`today_skin_id`),
    UNIQUE KEY `uq_today_skin_user_date` (`user_id`, `log_date`),
    CONSTRAINT `fk_ts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `ai_analysis`
(
    `analysis_id`   BIGINT NOT NULL AUTO_INCREMENT,
    `today_skin_id` BIGINT NOT NULL,
    `overall_score` INT    NULL COMMENT '종합 피부 점수',
    `ai_comment`    TEXT   NULL,
    `created_at`    DATETIME NOT NULL DEFAULT NOW(),
    `updated_at`    DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`analysis_id`),
    UNIQUE KEY `uq_analysis_today_skin` (`today_skin_id`),
    CONSTRAINT `fk_aa_today_skin` FOREIGN KEY (`today_skin_id`) REFERENCES `today_skin` (`today_skin_id`)
);

CREATE TABLE `detailed_skin_analysis`
(
    `detail_analysis_id` BIGINT   NOT NULL AUTO_INCREMENT,
    `analysis_id`        BIGINT   NOT NULL,
    `sebum_level`        INT      NULL,
    `redness`            INT      NULL,
    `hydration_level`    INT      NULL,
    `pore_condition`     INT      NULL,
    `skin_elasticity`    INT      NULL,
    `created_at`         DATETIME NOT NULL DEFAULT NOW(),
    `updated_at`         DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`detail_analysis_id`),
    UNIQUE KEY `uq_detail_analysis` (`analysis_id`),
    CONSTRAINT `fk_dsa_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `ai_analysis` (`analysis_id`)
);

CREATE TABLE `ai_daily_routine`
(
    `routine_id`       BIGINT                                                     NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT                                                     NOT NULL,
    `analysis_id`      BIGINT                                                     NOT NULL COMMENT 'n일 이내의 가장 최근 분석',
    `target_date`      DATE                                                       NOT NULL,
    `phase_type`       ENUM ('MENSTRUATION', 'FOLLICULAR', 'OVULATION', 'LUTEAL') NOT NULL COMMENT '오늘 날짜에 해당하는 주기 단계',
    `skincare_routine` TEXT                                                       NOT NULL,
    `action_content`   TEXT                                                       NOT NULL,
    `exercise_routine` TEXT                                                       NULL,
    `created_at`       DATETIME                                                   NOT NULL DEFAULT NOW(),
    `updated_at`       DATETIME                                                   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`routine_id`),
    CONSTRAINT `fk_adr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_adr_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `ai_analysis` (`analysis_id`)
);

CREATE TABLE `product`
(
    `product_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `prod_name`  VARCHAR(100) NULL,
    `purpose`    VARCHAR(50)  NULL,
    `price`      INT          NULL,
    `image_url`  VARCHAR(255) NULL,
    `created_at` DATETIME     NOT NULL DEFAULT NOW(),
    `updated_at` DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`product_id`)
);

CREATE TABLE `prod_recommend`
(
    `prod_recommend_id` BIGINT NOT NULL AUTO_INCREMENT,
    `analysis_id`       BIGINT NOT NULL,
    `product_id`        BIGINT NOT NULL,
    `match_percentage`  INT    NULL,
    PRIMARY KEY (`prod_recommend_id`),
    CONSTRAINT `fk_pr_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `ai_analysis` (`analysis_id`),
    CONSTRAINT `fk_pr_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

CREATE TABLE `ai_chat_room`
(
    `chat_room_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `analysis_id`  BIGINT       NULL,
    `title`        VARCHAR(100) NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT NOW(),
    `updated_at`   DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`chat_room_id`),
    CONSTRAINT `fk_acr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_acr_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `ai_analysis` (`analysis_id`)
);

CREATE TABLE `ai_chat_message`
(
    `chat_message_id` BIGINT                        NOT NULL AUTO_INCREMENT,
    `chat_room_id`    BIGINT                        NOT NULL,
    `role`            ENUM ('AI', 'USER')            NOT NULL COMMENT 'AI, 사용자',
    `message_type`    ENUM ('TEXT', 'IMAGE', 'FILE') NOT NULL DEFAULT 'TEXT' COMMENT '텍스트, 이미지, 파일',
    `content`         TEXT                          NULL     COMMENT '텍스트 내용',
    `file_url`        VARCHAR(255)                  NULL     COMMENT '이미지, 파일 경로 url',
    `created_at`      DATETIME                      NOT NULL DEFAULT NOW(),
    `updated_at`      DATETIME                      NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`chat_message_id`),
    CONSTRAINT `fk_acm_chat_room` FOREIGN KEY (`chat_room_id`) REFERENCES `ai_chat_room` (`chat_room_id`)
);
