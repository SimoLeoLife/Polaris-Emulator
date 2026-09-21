package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.variablefx.WiredVariableFxStyles;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Hearts or a health bar over whoever holds the variable on this tile. Registered on {@code wf_xtra_var_fx_health}. */
public class WiredExtraVariableFxHealthPoints extends WiredExtraVariableFx {
    public static final int CODE = 130;

    public WiredExtraVariableFxHealthPoints(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraVariableFxHealthPoints(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public int getCategory() {
        return WiredVariableFxStyles.CATEGORY_HEALTH_POINTS;
    }

    @Override
    public int getCode() {
        return CODE;
    }
}
