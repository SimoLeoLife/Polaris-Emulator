-- Habbo Club days as a talent track reward.
--
-- Re-runnable: a hotel that already carries the column (a manual pre-apply, or a run that was
-- applied but never recorded) must be able to record this version without failing on
-- "duplicate column", so the column is only added when information_schema does not list it.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements_talents' AND COLUMN_NAME = 'reward_hc_days') = 0,
    'ALTER TABLE `achievements_talents` ADD COLUMN `reward_hc_days` INT NOT NULL DEFAULT 0',
    'SELECT ''achievements_talents.reward_hc_days already present, skipping'' AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
