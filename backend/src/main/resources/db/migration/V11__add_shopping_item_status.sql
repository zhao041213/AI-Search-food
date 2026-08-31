ALTER TABLE shopping_item_checks
    ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'PENDING';

UPDATE shopping_item_checks
SET status = CASE WHEN checked = 1 THEN 'READY' ELSE 'PENDING' END;
