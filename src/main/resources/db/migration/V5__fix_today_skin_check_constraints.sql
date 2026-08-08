-- V1의 chk_diet_type은 옛날 단일값 enum('VEGETABLE_FOCUSED','FAST_FOOD','SPICY_FOOD','BALANCED','ALCOHOL') 기준이라
-- 현재 DietType enum(DAIRY, FRUIT, SPICY_FOOD, CAFFEINE, HIGH_FAT, SUGAR, SODA, ALCOHOL)과 맞지 않고,
-- 애플리케이션이 다중 선택값을 콤마로 이어붙여 저장하는 것도 고려하지 않았음.
--
-- V4_update_tody_skin_enums.sql은 diet_type 컬럼 길이(VARCHAR(100))만 바꿨을 뿐, 기존에 저장된
-- VEGETABLE_FOCUSED/FAST_FOOD/BALANCED 값은 그대로 남아있다. VEGETABLE_FOCUSED, BALANCED는 "긍정적인 식습관"
-- 개념이라 현재 DietType(자극 요인) enum에 대응되는 값이 없어 임의로 새 값에 매핑하면 과거 데이터 의미가
-- 왜곡될 수 있으므로, 데이터를 변형하지 않고 레거시 값을 새 제약조건에서도 계속 허용한다.
ALTER TABLE today_skin
    DROP CONSTRAINT IF EXISTS chk_diet_type;

ALTER TABLE today_skin
    ADD CONSTRAINT chk_diet_type CHECK (
        diet_type IS NULL OR diet_type ~
        '^(DAIRY|FRUIT|SPICY_FOOD|CAFFEINE|HIGH_FAT|SUGAR|SODA|ALCOHOL|VEGETABLE_FOCUSED|FAST_FOOD|BALANCED)(,(DAIRY|FRUIT|SPICY_FOOD|CAFFEINE|HIGH_FAT|SUGAR|SODA|ALCOHOL|VEGETABLE_FOCUSED|FAST_FOOD|BALANCED))*$'
        );

-- V4에서 skin_status 값을 DRY/OILY/TROUBLE/DULL로 갱신하며 today_skin_skin_status_check를 새로 추가했지만,
-- V1에서 만든 chk_skin_status(DRY/OILY/SENSITIVE/GOOD, 현재 SkinStatus enum과 불일치)를 지우지 않아 남아있었음.
ALTER TABLE today_skin
    DROP CONSTRAINT IF EXISTS chk_skin_status;
