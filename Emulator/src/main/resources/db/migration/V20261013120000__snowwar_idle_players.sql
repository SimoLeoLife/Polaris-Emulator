-- SnowWar idle players: warn a player who does nothing, then remove them.
-- timeout 0 switches it off; the warning comes this many seconds before removal.

INSERT INTO emulator_settings (`key`, `value`) VALUES
	('gamecenter.snowwar.idle.timeout.seconds', '60'),
	('gamecenter.snowwar.idle.warning.seconds', '15')
ON DUPLICATE KEY UPDATE `value` = `value`;
