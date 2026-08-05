-- 1. detailed_skin_analysis 컬럼명 변경
ALTER TABLE detailed_skin_analysis
    RENAME COLUMN sebum_level TO sebum;

ALTER TABLE detailed_skin_analysis
    RENAME COLUMN redness TO trouble;

ALTER TABLE detailed_skin_analysis
    RENAME COLUMN hydration_level TO moisture;

ALTER TABLE detailed_skin_analysis
    RENAME COLUMN pore_condition TO dullness;

ALTER TABLE detailed_skin_analysis
    RENAME COLUMN skin_elasticity TO elasticity;

-- 2. ai_analysis에 phase_comment 추가
ALTER TABLE ai_analysis
    ADD COLUMN phase_comment VARCHAR(255) NULL;
