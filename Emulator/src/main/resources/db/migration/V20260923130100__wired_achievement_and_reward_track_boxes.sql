-- The achievement and reward-track wired boxes: progress achievement, the achievement enabler
-- add-on, progress reward track and reset reward track.
--
-- They hand out hotel-wide progress, so they stay off until the hotel turns them on and lists what
-- rooms may use. The existing keys of a hotel that set them by hand are kept.
INSERT INTO `wired_emulator_settings` (`key`, `value`, `comment`) VALUES
    ('hotel.wired.achievements.enabled', '0', 'Let the progress-achievement wired box progress achievements (0/1).'),
    ('hotel.wired.achievements.allowed', '', 'Achievement names wired may progress, comma separated. Empty allows none.'),
    ('hotel.wired.achievements.max_per_window', '50', 'Progress wired may give one user per achievement per window.'),
    ('hotel.wired.achievements.window_seconds', '3600', 'Length of the achievement progress window in seconds.'),
    ('hotel.wired.reward_tracks.enabled', '0', 'Let the reward-track wired boxes progress and reset reward tracks (0/1).'),
    ('hotel.wired.reward_tracks.allowed', '', 'Reward tracks (track) or single tasks (track:task) wired may move, comma separated. Empty allows none.'),
    ('hotel.wired.reward_tracks.max_per_window', '50', 'Task progress wired may give one user per track per window.'),
    ('hotel.wired.reward_tracks.window_seconds', '3600', 'Length of the reward-track progress window in seconds.')
ON DUPLICATE KEY UPDATE `value` = `value`;

-- Furniture rows. Fixed sprite IDs follow the other Polaris wired furni; a hotel that already
-- carries a row keeps it and only has interaction_type realigned. The 0/1 flags are quoted for the
-- enum('0','1') columns of Arcturus-derived hotels. No catalogue offers: these are staff furni.
INSERT INTO `items_base`
    (`sprite_id`, `public_name`, `item_name`, `type`, `width`, `length`, `stack_height`,
     `allow_stack`, `allow_sit`, `allow_lay`, `allow_walk`, `allow_gift`, `allow_trade`,
     `allow_recycle`, `allow_marketplace_sell`, `allow_inventory_stack`, `interaction_type`,
     `interaction_modes_count`, `vending_ids`, `multiheight`, `customparams`)
SELECT 2000029852, 'WIRED Effect: Progress Achievement', 'wf_act_progress_achievement', 's', 1, 1, 0.65,
       '1', '0', '0', '1', '1', '1', '0', '0', '1', 'wf_act_progress_achievement', 2, '0', '', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `items_base` WHERE `item_name` = 'wf_act_progress_achievement'
);

INSERT INTO `items_base`
    (`sprite_id`, `public_name`, `item_name`, `type`, `width`, `length`, `stack_height`,
     `allow_stack`, `allow_sit`, `allow_lay`, `allow_walk`, `allow_gift`, `allow_trade`,
     `allow_recycle`, `allow_marketplace_sell`, `allow_inventory_stack`, `interaction_type`,
     `interaction_modes_count`, `vending_ids`, `multiheight`, `customparams`)
SELECT 2000029853, 'WIRED Add-on: Achievement Enabler', 'wf_xtra_achievement_enabler', 's', 1, 1, 0.65,
       '1', '0', '0', '1', '1', '1', '0', '0', '1', 'wf_xtra_achievement_enabler', 2, '0', '', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `items_base` WHERE `item_name` = 'wf_xtra_achievement_enabler'
);

INSERT INTO `items_base`
    (`sprite_id`, `public_name`, `item_name`, `type`, `width`, `length`, `stack_height`,
     `allow_stack`, `allow_sit`, `allow_lay`, `allow_walk`, `allow_gift`, `allow_trade`,
     `allow_recycle`, `allow_marketplace_sell`, `allow_inventory_stack`, `interaction_type`,
     `interaction_modes_count`, `vending_ids`, `multiheight`, `customparams`)
SELECT 2000029854, 'WIRED Effect: Progress Reward Track', 'wf_act_progress_reward_track', 's', 1, 1, 0.65,
       '1', '0', '0', '1', '1', '1', '0', '0', '1', 'wf_act_progress_reward_track', 2, '0', '', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `items_base` WHERE `item_name` = 'wf_act_progress_reward_track'
);

INSERT INTO `items_base`
    (`sprite_id`, `public_name`, `item_name`, `type`, `width`, `length`, `stack_height`,
     `allow_stack`, `allow_sit`, `allow_lay`, `allow_walk`, `allow_gift`, `allow_trade`,
     `allow_recycle`, `allow_marketplace_sell`, `allow_inventory_stack`, `interaction_type`,
     `interaction_modes_count`, `vending_ids`, `multiheight`, `customparams`)
SELECT 2000029855, 'WIRED Effect: Reset Reward Track', 'wf_act_reset_reward_track', 's', 1, 1, 0.65,
       '1', '0', '0', '1', '1', '1', '0', '0', '1', 'wf_act_reset_reward_track', 2, '0', '', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `items_base` WHERE `item_name` = 'wf_act_reset_reward_track'
);

UPDATE `items_base` SET `interaction_type` = 'wf_act_progress_achievement'
    WHERE `item_name` = 'wf_act_progress_achievement' AND `interaction_type` <> 'wf_act_progress_achievement';

UPDATE `items_base` SET `interaction_type` = 'wf_xtra_achievement_enabler'
    WHERE `item_name` = 'wf_xtra_achievement_enabler' AND `interaction_type` <> 'wf_xtra_achievement_enabler';

UPDATE `items_base` SET `interaction_type` = 'wf_act_progress_reward_track'
    WHERE `item_name` = 'wf_act_progress_reward_track' AND `interaction_type` <> 'wf_act_progress_reward_track';

UPDATE `items_base` SET `interaction_type` = 'wf_act_reset_reward_track'
    WHERE `item_name` = 'wf_act_reset_reward_track' AND `interaction_type` <> 'wf_act_reset_reward_track';
