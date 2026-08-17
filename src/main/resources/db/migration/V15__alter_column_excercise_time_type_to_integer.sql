ALTER TABLE today_skin
ALTER COLUMN exercise_time TYPE INTEGER USING
    CASE exercise_time
        WHEN 'ZERO_M' THEN 0
        WHEN 'THIRTY_M' THEN 30
        WHEN 'SIXTY_M' THEN 60
        WHEN 'NINETY_M_PLUS' THEN 90
        ELSE NULL
END;