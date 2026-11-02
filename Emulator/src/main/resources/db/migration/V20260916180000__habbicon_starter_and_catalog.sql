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

-- Re-runnable: the page and its offers are only created where they are missing, so a hotel that
-- already has a Habbicons page (a manual pre-apply, or a run that was applied but never
-- recorded) keeps that page and any offers an operator has since edited or moved.
INSERT INTO catalog_pages (parent_id, caption_save, caption, page_layout, icon_image, min_rank, order_num)
SELECT -1, 'habbicons', 'Habbicons', 'default_3x3', 107, 1, 6
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM catalog_pages existing_page
    WHERE existing_page.caption_save = 'habbicons'
);

SET @habbicon_page = (
    SELECT MIN(id) FROM catalog_pages
    WHERE caption_save = 'habbicons'
);

INSERT INTO catalog_items (
    item_ids, page_id, catalog_name, cost_credits, cost_points, points_type,
    amount, order_number, offer_id, have_offer, habbicon_id)
SELECT '0', @habbicon_page, name, cost_credits, cost_points, points_type,
       1, id, -1, '1', id
FROM habbicons
WHERE (cost_credits > 0 OR cost_points > 0)
  AND NOT EXISTS (
      SELECT 1 FROM catalog_items existing_offer
      WHERE existing_offer.habbicon_id = habbicons.id
  );
