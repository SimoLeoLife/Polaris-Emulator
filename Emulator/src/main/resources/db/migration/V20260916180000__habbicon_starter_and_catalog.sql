-- V20260911150000 granted every existing user every Habbicon and left every icon priced at
-- zero, which HabbiconService reads as "free to claim". Both are reversed here: a single
-- starter icon, a price on everything else, and a catalog page to buy them from.

-- Only revoke holdings that are exactly the untouched backfill: the full icon set, none of it
-- favourited, used or still flagged unseen. A user who has interacted with any icon keeps all
-- of theirs, so no purchased or earned Habbicon can be taken away here.
DELETE grant_row FROM users_habbicons grant_row
JOIN (
    SELECT user_id
    FROM users_habbicons
    GROUP BY user_id
    HAVING COUNT(*) = (SELECT COUNT(*) FROM habbicons)
       AND SUM(state <> 2 OR unseen <> FALSE OR last_used <> 0) = 0
) backfilled ON backfilled.user_id = grant_row.user_id;

-- duck_duck is the starter; every other icon is earned, bought or a set reward.
UPDATE habbicons SET default_owned = (id = 28);

UPDATE habbicons SET cost_credits = 5
WHERE default_owned = FALSE
  AND id NOT IN (SELECT reward_id FROM habbicon_collections);

UPDATE habbicon_collections SET cost_credits = 40;

INSERT INTO catalog_pages (parent_id, caption_save, caption, page_layout, icon_image, min_rank, order_num)
VALUES (-1, 'habbicons', 'Habbicons', 'default_3x3', 107, 1, 6);

SET @habbicon_page = LAST_INSERT_ID();

INSERT INTO catalog_items (
    item_ids, page_id, catalog_name, cost_credits, cost_points, points_type,
    amount, order_number, offer_id, have_offer, habbicon_id)
SELECT '0', @habbicon_page, name, cost_credits, cost_points, points_type,
       1, id, -1, '1', id
FROM habbicons
WHERE cost_credits > 0 OR cost_points > 0;
