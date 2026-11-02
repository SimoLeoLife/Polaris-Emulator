-- Habbo's recolourable Builders Club furni (*_recolorable) use interaction_type 'recolorable': the furni
-- keeps its own colour (extra_data "state|r,g,b") and can be recoloured from the infostand.
-- The old colour variants (bc_x*N) are not touched.
UPDATE `items_base`
SET `interaction_type` = 'recolorable'
WHERE `item_name` LIKE '%\_recolorable'
  AND `interaction_type` IN ('', 'default');
