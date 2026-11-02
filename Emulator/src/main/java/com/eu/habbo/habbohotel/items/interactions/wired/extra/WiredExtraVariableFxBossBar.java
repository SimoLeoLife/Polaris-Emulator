package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.variablefx.WiredVariableFxStyles;
import java.sql.ResultSet;
import java.sql.SQLException;

/** The wide boss health bar of the variable on this tile. Registered on {@code wf_xtra_var_fx_boss}. */
public class WiredExtraVariableFxBossBar extends WiredExtraVariableFx {
    public static final int CODE = 134;

    public WiredExtraVariableFxBossBar(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraVariableFxBossBar(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public int getCategory() {
        return WiredVariableFxStyles.CATEGORY_BOSS_BAR;
    }

    @Override
    public int getCode() {
        return CODE;
    }
}
