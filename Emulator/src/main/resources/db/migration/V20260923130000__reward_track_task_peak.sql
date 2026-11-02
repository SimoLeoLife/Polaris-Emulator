-- The highest count each reward-track task reached for a user. Task levels pay only past it, so a
-- task that the reset-reward-track wired box puts back to zero never pays the same level twice.
-- Additive and idempotent; existing rows start at their current progress.
ALTER TABLE `users_reward_track_tasks` ADD COLUMN IF NOT EXISTS `peak_count` INT NOT NULL DEFAULT 0;

UPDATE `users_reward_track_tasks` SET `peak_count` = `progress_count` WHERE `peak_count` < `progress_count`;
