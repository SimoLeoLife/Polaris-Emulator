-- AIR achievement states: disabled, enabled, archived, off-season, wired-controlled.
--
-- Re-runnable: a hotel that already carries these columns (a manual pre-apply, or a run that
-- was applied but never recorded) must be able to record this version without failing on
-- "duplicate column". Each column is only added when information_schema does not list it.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements' AND COLUMN_NAME = 'state') = 0,
    'ALTER TABLE `achievements` ADD COLUMN `state` SMALLINT NOT NULL DEFAULT 1',
    'SELECT ''achievements.state already present, skipping'' AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements' AND COLUMN_NAME = 'display_method') = 0,
    'ALTER TABLE `achievements` ADD COLUMN `display_method` INT NOT NULL DEFAULT 0',
    'SELECT ''achievements.display_method already present, skipping'' AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements' AND COLUMN_NAME = 'subcategory') = 0,
    'ALTER TABLE `achievements` ADD COLUMN `subcategory` VARCHAR(64) NOT NULL DEFAULT ''''',
    'SELECT ''achievements.subcategory already present, skipping'' AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Widening the category enum is safe to repeat: every existing value stays valid.
ALTER TABLE achievements
    MODIFY COLUMN category ENUM('identity','explore','music','social','games','room_builder','pets','tools','events','other','test','invisible','misc','archive','wired_games') NOT NULL DEFAULT 'identity';

-- Legacy seeds used MyISAM; progress and queued events must participate in reward transactions.
-- Only rebuilt when not already InnoDB, so a re-run does not lock the tables for nothing.
SET @engine := (SELECT ENGINE FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users_achievements');
SET @ddl := IF(@engine <> 'InnoDB',
    'ALTER TABLE `users_achievements` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @engine := (SELECT ENGINE FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users_achievements_queue');
SET @ddl := IF(@engine <> 'InnoDB',
    'ALTER TABLE `users_achievements_queue` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
