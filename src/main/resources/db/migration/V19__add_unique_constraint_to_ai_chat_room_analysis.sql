ALTER TABLE ai_chat_room ADD CONSTRAINT uq_acr_user_analysis UNIQUE (user_id, analysis_id);
