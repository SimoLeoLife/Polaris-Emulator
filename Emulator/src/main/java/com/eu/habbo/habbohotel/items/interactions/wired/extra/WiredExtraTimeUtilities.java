package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.core.WiredDerivedVariableBox;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

/**
 * Time-utilities add-on (furni classname {@code wf_xtra_var_time_util}). Placed on the tile of a variable
 * definition, it reads the variable as a point in time (its value as unix seconds, its creation time or its
 * last update time) and exposes read-only sub-variables: calendar parts in the room's wired timezone and
 * whole time units counted from 1970.
 *
 * <p>Int params {@code [mask, mode]}. Bit {@code id} of the mask selects the sub-variable with that id:
 * calendar parts 1-10, units 20-26.</p>
 */
public class WiredExtraTimeUtilities extends InteractionWiredExtra implements WiredDerivedVariableBox {
    public static final int CODE = 98;

    public static final int UNIT_MILLISECONDS = 0;
    public static final int UNIT_SECONDS = 1;
    public static final int UNIT_MINUTES = 2;
    public static final int UNIT_HOURS = 3;

    public static final int MODE_VALUE = 0;
    public static final int MODE_CREATION_TIME = 1;
    public static final int MODE_LAST_UPDATE_TIME = 2;

    public static final int SUB_MILLISECOND_OF_SECOND = 1;
    public static final int SUB_SECONDS_OF_MINUTE = 2;
    public static final int SUB_MINUTE_OF_HOUR = 3;
    public static final int SUB_HOUR_OF_DAY = 4;
    public static final int SUB_DAY_OF_WEEK = 5;
    public static final int SUB_DAY_OF_MONTH = 6;
    public static final int SUB_DAY_OF_YEAR = 7;
    public static final int SUB_WEEK_OF_YEAR = 8;
    public static final int SUB_MONTH_OF_YEAR = 9;
    public static final int SUB_YEAR = 10;
    public static final int SUB_MILLISECONDS = 20;
    public static final int SUB_SECONDS = 21;
    public static final int SUB_MINUTES = 22;
    public static final int SUB_HOURS = 23;
    public static final int SUB_DAYS = 24;
    public static final int SUB_WEEKS = 25;
    public static final int SUB_MONTHS = 26;

    private static final int CALENDAR_MASK = 0x7FE;
    private static final int UNITS_MASK = 0x7F00000;
    private static final int VALID_MASK = CALENDAR_MASK | UNITS_MASK;
    private static final int MAX_SUB_ID = SUB_MONTHS;

    private int timeUnit = UNIT_SECONDS;
    private int subvariableMask = 0;
    private int mode = MODE_VALUE;

    public WiredExtraTimeUtilities(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraTimeUtilities(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        int[] params = settings.getIntParams();
        this.subvariableMask = normalizeMask((params.length > 0) ? params[0] : 0);
        this.mode = normalizeMode((params.length > 1) ? params[1] : MODE_VALUE);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.timeUnit, this.subvariableMask, this.mode));
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.subvariableMask);
        message.appendInt(this.mode);
        message.appendInt(0);
        message.appendInt(CODE);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.onPickUp();

        String wiredData = set.getString("wired_data");
        if (wiredData == null || wiredData.isEmpty()) {
            return;
        }

        if (wiredData.startsWith("{")) {
            JsonData data = WiredExtraPayloadGuard.fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.timeUnit = normalizeUnit(data.timeUnit);
                this.subvariableMask = normalizeMask(data.subvariableMask);
                this.mode = normalizeMode(data.mode);
            }
            return;
        }

        try {
            this.timeUnit = normalizeUnit(Integer.parseInt(wiredData));
        } catch (NumberFormatException ignored) {
            this.timeUnit = UNIT_SECONDS;
        }
    }

    @Override
    public void onPickUp() {
        this.timeUnit = UNIT_SECONDS;
        this.subvariableMask = 0;
        this.mode = MODE_VALUE;
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {}

    @Override
    public boolean hasConfiguration() {
        return true;
    }

    /** The unit older saves stored; nothing reads it at runtime. */
    public int getTimeUnit() {
        return this.timeUnit;
    }

    public int getSubvariableMask() {
        return this.subvariableMask;
    }

    public int getMode() {
        return this.mode;
    }

    /** Whether the sub-variables can be read from a variable that holds no value. */
    public boolean readsTimestamps() {
        return this.mode != MODE_VALUE;
    }

    @Override
    public List<Integer> getSelectedSubvariables() {
        List<Integer> result = new ArrayList<>();
        for (int id = 1; id <= MAX_SUB_ID; id++) {
            if (this.hasSubvariable(id)) result.add(id);
        }
        return result;
    }

    @Override
    public boolean hasSubvariable(int subType) {
        return subType > 0 && subType <= MAX_SUB_ID && (this.subvariableMask & (1 << subType)) != 0;
    }

    @Override
    public String subvariableKey(int subType) {
        return switch (subType) {
            case SUB_MILLISECOND_OF_SECOND -> "millisecond_of_second";
            case SUB_SECONDS_OF_MINUTE -> "seconds_of_minute";
            case SUB_MINUTE_OF_HOUR -> "minute_of_hour";
            case SUB_HOUR_OF_DAY -> "hour_of_day";
            case SUB_DAY_OF_WEEK -> "day_of_week";
            case SUB_DAY_OF_MONTH -> "day_of_month";
            case SUB_DAY_OF_YEAR -> "day_of_year";
            case SUB_WEEK_OF_YEAR -> "week_of_year";
            case SUB_MONTH_OF_YEAR -> "month_of_year";
            case SUB_YEAR -> "year";
            case SUB_MILLISECONDS -> "millisecond";
            case SUB_SECONDS -> "second";
            case SUB_MINUTES -> "minute";
            case SUB_HOURS -> "hour";
            case SUB_DAYS -> "day";
            case SUB_WEEKS -> "week";
            case SUB_MONTHS -> "month";
            default -> "value";
        };
    }

    @Override
    public Integer derive(int subType, Integer baseValue) {
        return this.derive(subType, baseValue, 0, 0, ZoneOffset.UTC);
    }

    /** The sub-variable for a variable's value and timestamps (unix seconds), calendar parts in {@code zone}. */
    public Integer derive(int subType, Integer value, int createdAt, int updatedAt, ZoneId zone) {
        Long seconds = this.resolveSeconds(value, createdAt, updatedAt);
        return (seconds != null) ? deriveFromSeconds(subType, seconds, zone) : null;
    }

    private Long resolveSeconds(Integer value, int createdAt, int updatedAt) {
        return switch (this.mode) {
            case MODE_CREATION_TIME -> (createdAt > 0) ? (long) createdAt : null;
            case MODE_LAST_UPDATE_TIME -> (updatedAt > 0) ? (long) updatedAt : null;
            default -> (value != null) ? (long) Math.max(0, value) : null;
        };
    }

    static Integer deriveFromSeconds(int subType, long seconds, ZoneId zone) {
        if (subType >= SUB_MILLISECONDS) {
            return switch (subType) {
                case SUB_MILLISECONDS -> clamp(seconds * 1000L);
                case SUB_SECONDS -> clamp(seconds);
                case SUB_MINUTES -> clamp(Math.floorDiv(seconds, 60L));
                case SUB_HOURS -> clamp(Math.floorDiv(seconds, 3_600L));
                case SUB_DAYS -> clamp(Math.floorDiv(seconds, 86_400L));
                case SUB_WEEKS -> clamp(Math.floorDiv(seconds, 604_800L));
                case SUB_MONTHS -> {
                    ZonedDateTime utc = Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC);
                    yield clamp((utc.getYear() - 1970L) * 12L + utc.getMonthValue() - 1L);
                }
                default -> null;
            };
        }

        ZonedDateTime local = Instant.ofEpochSecond(seconds).atZone((zone != null) ? zone : ZoneOffset.UTC);
        return switch (subType) {
            case SUB_MILLISECOND_OF_SECOND -> local.getNano() / 1_000_000;
            case SUB_SECONDS_OF_MINUTE -> local.getSecond();
            case SUB_MINUTE_OF_HOUR -> local.getMinute();
            case SUB_HOUR_OF_DAY -> local.getHour();
            case SUB_DAY_OF_WEEK -> local.getDayOfWeek().getValue();
            case SUB_DAY_OF_MONTH -> local.getDayOfMonth();
            case SUB_DAY_OF_YEAR -> local.getDayOfYear();
            case SUB_WEEK_OF_YEAR -> local.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            case SUB_MONTH_OF_YEAR -> local.getMonthValue();
            case SUB_YEAR -> local.getYear();
            default -> null;
        };
    }

    private static int clamp(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static int normalizeMask(int value) {
        return value & VALID_MASK;
    }

    private static int normalizeMode(int value) {
        return (value == MODE_CREATION_TIME || value == MODE_LAST_UPDATE_TIME) ? value : MODE_VALUE;
    }

    private static int normalizeUnit(int value) {
        return Math.max(UNIT_MILLISECONDS, Math.min(UNIT_HOURS, value));
    }

    static class JsonData {
        int timeUnit = UNIT_SECONDS;
        int subvariableMask;
        int mode;

        JsonData() {}

        JsonData(int timeUnit, int subvariableMask, int mode) {
            this.timeUnit = timeUnit;
            this.subvariableMask = subvariableMask;
            this.mode = mode;
        }
    }
}
