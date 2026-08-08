-- V1의 chk_diet_type은 옛날 단일값 enum('VEGETABLE_FOCUSED','FAST_FOOD','SPICY_FOOD','BALANCED','ALCOHOL') 기준이라
-- 현재 DietType enum(DAIRY, FRUIT, SPICY_FOOD, CAFFEINE, HIGH_FAT, SUGAR, SODA, ALCOHOL)과 맞지 않고,
-- 애플리케이션이 다중 선택값을 콤마로 이어붙여 저장하는 것도 고려하지 않았음. 삭제 후 재정의.
ALTER TABLE today_skin
    DROP CONSTRAINT IF EXISTS chk_diet_type;

ALTER TABLE today_skin
    ADD CONSTRAINT chk_diet_type CHECK (
        diet_type IS NULL OR diet_type ~
        '^(DAIRY|FRUIT|SPICY_FOOD|CAFFEINE|HIGH_FAT|SUGAR|SODA|ALCOHOL)(,(DAIRY|FRUIT|SPICY_FOOD|CAFFEINE|HIGH_FAT|SUGAR|SODA|ALCOHOL))*$'
        );

-- V4에서 skin_status 값을 DRY/OILY/TROUBLE/DULL로 갱신하며 today_skin_skin_status_check를 새로 추가했지만,
-- V1에서 만든 chk_skin_status(DRY/OILY/SENSITIVE/GOOD, 현재 SkinStatus enum과 불일치)를 지우지 않아 남아있었음.
ALTER TABLE today_skin
    DROP CONSTRAINT IF EXISTS chk_skin_status;