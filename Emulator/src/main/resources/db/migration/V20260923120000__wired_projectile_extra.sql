-- The projectile add-on, sold as wf_xtra_rotate_to_dir, had no class behind it, so hotels that carry
-- the furni loaded it as 'default': an inert box that saved nothing and turned nothing. Point any
-- existing row at the interaction that now handles it. Idempotent: only rows that still differ are
-- touched, and a hotel without the furni is left alone.
UPDATE items_base SET interaction_type = 'wf_xtra_rotate_to_dir'
    WHERE item_name = 'wf_xtra_rotate_to_dir' AND interaction_type <> 'wf_xtra_rotate_to_dir';
