ALTER TABLE today_skin
ALTER COLUMN skin_status TYPE VARCHAR(20);

ALTER TABLE today_skin
DROP CONSTRAINT IF EXISTS today_skin_skin_status_check;

ALTER TABLE today_skin ADD CONSTRAINT today_skin_skin_status_check
    CHECK (skin_status IN ('DRY', 'OILY', 'TROUBLE', 'DULL'));

ALTER TABLE today_skin
ALTER COLUMN diet_type TYPE VARCHAR(100);

ALTER TABLE today_skin
DROP CONSTRAINT IF EXISTS today_skin_diet_type_check;