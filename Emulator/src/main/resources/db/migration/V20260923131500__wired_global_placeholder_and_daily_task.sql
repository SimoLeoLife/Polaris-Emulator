-- The global placeholder add-on and the daily task variable box had no class behind them, so a hotel
-- carrying either furni loaded it as 'default': an inert box. Point any existing row at the
-- interaction that now handles it. Idempotent: only rows that still differ are touched, and a hotel
-- without the furni is left alone.
--   wf_xtra_text_output_global -> WiredExtraTextOutputGlobal (extra, dialog code 2000)
--   wf_var_daily_task          -> WiredExtraDailyTask        (extra, dialog code 2008)

UPDATE items_base SET interaction_type = 'wf_xtra_text_output_global'
    WHERE item_name = 'wf_xtra_text_output_global' AND interaction_type <> 'wf_xtra_text_output_global';

UPDATE items_base SET interaction_type = 'wf_var_daily_task'
    WHERE item_name = 'wf_var_daily_task' AND interaction_type <> 'wf_var_daily_task';
