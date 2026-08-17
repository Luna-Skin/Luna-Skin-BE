ALTER TABLE today_skin
ALTER COLUMN water_intake TYPE NUMERIC(4,1) USING water_intake::NUMERIC;