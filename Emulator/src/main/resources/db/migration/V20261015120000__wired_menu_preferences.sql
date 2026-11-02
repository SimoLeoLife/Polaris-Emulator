-- The official wired menu keeps its preferences on the server (menu button, inspect button,
-- play-test mode, show all notifications, ui style). Stored as one short text; empty until the
-- user saves them once, so clients keep their local defaults until then.
ALTER TABLE `users_settings`
    ADD COLUMN IF NOT EXISTS `wired_menu_preferences` VARCHAR(64) NOT NULL DEFAULT '' AFTER `wired_whisper_disabled`;
