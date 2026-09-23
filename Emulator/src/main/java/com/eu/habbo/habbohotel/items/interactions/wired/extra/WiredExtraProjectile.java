package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredProjectileDirections;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WiredExtraProjectile extends InteractionWiredExtra {
    public static final int CODE = 136;

    public static final int PARAM_COUNT = 19;
    public static final int PARAM_ROTATE = 0;
    public static final int PARAM_DIRECTIONAL_SYSTEM = 1;
    public static final int PARAM_ROTATION_OFFSET = 10;

    /** The bounds of the editor's inputs, per param; a value outside them is clamped on save. */
    private static final int[][] PARAM_BOUNDS = {
        {0, 1}, // rotate in moving direction
        {0, 3}, // directional system
        {0, 1}, // scale animation time with distance
        {0, 1}, // time per tile is a variable
        {0, 100_000}, // time per tile (ms)
        {0, 3}, // time per tile variable target
        {0, 1}, // distance by x
        {0, 1}, // distance by y
        {0, 1}, // distance by height
        {0, 100_000}, // speed increase (ms)
        {0, 7}, // rotation offset (eighth turns)
        {0, 127}, // internal variables mask
        {0, 1}, // change the shooter's direction
        {0, 1}, // bunny hop
        {0, 2}, // distance mode: normal, overshoot, fixed
        {0, 1}, // distance tiles is a variable
        {-64, 64}, // distance tiles
        {0, 3}, // distance tiles variable target
        {-1000, 1000} // curve strength
    };

    private final Set<HabboItem> items = new LinkedHashSet<>();
    private int[] params = defaultParams();

    public WiredExtraProjectile(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraProjectile(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        Room room = settings.getRoom();

        if (room == null || settings.getFurniIds().length > WiredManager.MAXIMUM_FURNI_SELECTION) {
            return false;
        }

        this.params = normalizeParams(settings.getIntParams());
        this.items.clear();

        for (int itemId : settings.getFurniIds()) {
            HabboItem item = room.getHabboItem(itemId);

            if (item != null) {
                this.items.add(item);
            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson()
                .toJson(new JsonData(
                        this.params.clone(),
                        this.items.stream().map(HabboItem::getId).collect(Collectors.toList())));
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.refresh(room);

        message.appendBoolean(false);
        message.appendInt(WiredManager.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());

        for (HabboItem item : this.items) {
            message.appendInt(item.getId());
        }

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(PARAM_COUNT);

        for (int param : this.params) {
            message.appendInt(param);
        }

        message.appendInt(0);
        message.appendInt(CODE);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.onPickUp();

        String wiredData = set.getString("wired_data");
        if (wiredData == null || !wiredData.startsWith("{")) {
            return;
        }

        JsonData data = WiredExtraPayloadGuard.fromJson(wiredData, JsonData.class);
        if (data == null) {
            return;
        }

        this.params = normalizeParams(data.params);

        if (data.itemIds != null && room != null) {
            for (Integer itemId : data.itemIds) {
                HabboItem item = (itemId == null) ? null : room.getHabboItem(itemId);

                if (item != null) {
                    this.items.add(item);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.params = defaultParams();
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) {}

    @Override
    public boolean hasConfiguration() {
        return true;
    }

    public boolean appliesTo(HabboItem movingItem) {
        if (movingItem == null) {
            return false;
        }

        if (this.items.isEmpty()) {
            return true;
        }

        for (HabboItem item : this.items) {
            if (item.getId() == movingItem.getId()) {
                return true;
            }
        }

        return false;
    }

    public int resolveRotation(int fromX, int fromY, int toX, int toY, int fallbackRotation) {
        if (this.params[PARAM_ROTATE] == 0) {
            return fallbackRotation;
        }

        int facing = WiredProjectileDirections.resolve(this.params[PARAM_DIRECTIONAL_SYSTEM], toX - fromX, toY - fromY);

        if (facing < 0) {
            return fallbackRotation;
        }

        return WiredProjectileDirections.rotate(facing, this.params[PARAM_ROTATION_OFFSET]);
    }

    public int[] getParams() {
        return this.params.clone();
    }

    public Set<HabboItem> getItems() {
        return this.items;
    }

    private void refresh(Room room) {
        if (room == null) {
            return;
        }

        this.items.removeIf(item -> room.getHabboItem(item.getId()) == null);
    }

    private static int[] defaultParams() {
        int[] defaults = new int[PARAM_COUNT];
        defaults[PARAM_ROTATE] = 1;
        return defaults;
    }

    static int[] normalizeParams(int[] raw) {
        int[] normalized = defaultParams();

        if (raw == null) {
            return normalized;
        }

        for (int index = 0; index < PARAM_COUNT && index < raw.length; index++) {
            normalized[index] = Math.max(PARAM_BOUNDS[index][0], Math.min(PARAM_BOUNDS[index][1], raw[index]));
        }

        return normalized;
    }

    static final class JsonData {
        int[] params;
        List<Integer> itemIds;

        JsonData(int[] params, List<Integer> itemIds) {
            this.params = params;
            this.itemIds = itemIds;
        }
    }
}
