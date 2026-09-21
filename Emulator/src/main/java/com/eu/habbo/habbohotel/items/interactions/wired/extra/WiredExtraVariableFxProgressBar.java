package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.variablefx.WiredVariableFxStyles;
import java.sql.ResultSet;
import java.sql.SQLException;

/** A plain progress bar of the variable on this tile between its minimum and maximum. Registered on {@code wf_xtra_var_fx_progress}. */
public class WiredExtraVariableFxProgressBar extends WiredExtraVariableFx {
    public static final int CODE = 131;

    public WiredExtraVariableFxProgressBar(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraVariableFxProgressBar(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public int getCategory() {
        return WiredVariableFxStyles.CATEGORY_PROGRESS_BAR;
    }

    @Override
    public int getCode() {
        return CODE;
    }
}
