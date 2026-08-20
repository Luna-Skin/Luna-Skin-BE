ALTER TABLE today_skin
ALTER COLUMN sleep_time TYPE NUMERIC(3,1) USING sleep_time::NUMERIC;
