package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The official {@code wf_trg_state_changed} trigger: fires on any state update of the picked furni,
 * whether a user toggled it or a wired effect set it. Same dialog and stored data as the user-toggle
 * box ({@code wf_trg_stuff_state}), so a placed box keeps its settings; only what it listens to
 * differs. A stack that toggles the furni it watches fires itself again, which the execution guard
 * cuts off like any other loop.
 */
public class WiredTriggerFurniStateUpdated extends WiredTriggerFurniStateToggled {
    private static final WiredTriggerType type = WiredTriggerType.STATE_CHANGE;

    public WiredTriggerFurniStateUpdated(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerFurniStateUpdated(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean matches(HabboItem triggerItem, WiredEvent event) {
        // Effect-caused changes are the point of this box, so the parent's guard does not apply.
        return this.matchesStateChange(event);
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }
}
