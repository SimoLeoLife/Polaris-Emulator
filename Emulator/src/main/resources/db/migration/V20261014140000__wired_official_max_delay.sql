-- The official wired editor offers action delays of 0 to 120 pulses (half seconds, so up to 60 s).
-- Only hotels still on the old default of 20 are raised; a value someone chose stays.
UPDATE `wired_emulator_settings`
SET `value` = '120',
    `comment` = 'Maximum action delay in pulses of 0.5 s; the official editor allows 0-120 (60 s).'
WHERE `key` = 'hotel.wired.max_delay'
  AND `value` = '20';
