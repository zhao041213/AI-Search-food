ALTER TABLE feature_suggestions MODIFY COLUMN title TEXT NOT NULL;
ALTER TABLE feature_suggestions MODIFY COLUMN detail TEXT NOT NULL;
ALTER TABLE feature_suggestions MODIFY COLUMN expected_effect TEXT NULL;
ALTER TABLE feature_suggestions MODIFY COLUMN internal_note TEXT NULL;
ALTER TABLE feature_suggestions MODIFY COLUMN admin_reply TEXT NULL;
ALTER TABLE feature_suggestion_additions MODIFY COLUMN content TEXT NOT NULL;
