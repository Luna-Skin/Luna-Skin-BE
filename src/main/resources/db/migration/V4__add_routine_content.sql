-- 1. 루틴 콘텐츠 테이블
CREATE TABLE routine_content (
    content_id BIGSERIAL PRIMARY KEY,
    category   VARCHAR(20)  NOT NULL,
    content    VARCHAR(100) NOT NULL,
    CONSTRAINT chk_content_category CHECK (category IN ('SKINCARE', 'ACTION', 'EXERCISE'))
);

-- 2. ai_daily_routine 수정
ALTER TABLE ai_daily_routine
    ALTER COLUMN analysis_id DROP NOT NULL,
    DROP COLUMN skincare_routine,
    DROP COLUMN action_content,
    DROP COLUMN exercise_routine,
    ADD COLUMN skincare_content_id BIGINT REFERENCES routine_content(content_id),
    ADD COLUMN action_content_id   BIGINT REFERENCES routine_content(content_id),
    ADD COLUMN exercise_content_id BIGINT REFERENCES routine_content(content_id),
    ADD CONSTRAINT uq_adr_user_date UNIQUE (user_id, target_date);
