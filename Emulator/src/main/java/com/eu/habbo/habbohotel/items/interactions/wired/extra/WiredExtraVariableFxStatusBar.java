package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.variablefx.WiredVariableFxStyles;
import java.sql.ResultSet;
import java.sql.SQLException;

/** A themed bar (energy, mana, cooldown...) of the variable on this tile; the style brings the icon and colour. Registered on {@code wf_xtra_var_fx_status}. */
public class WiredExtraVariableFxStatusBar extends WiredExtraVariableFx {
    public static final int CODE = 133;

    public WiredExtraVariableFxStatusBar(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraVariableFxStatusBar(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public int getCategory() {
        return WiredVariableFxStyles.CATEGORY_STATUS_BAR;
    }

    @Override
    public int getCode() {
        return CODE;
    }
}
