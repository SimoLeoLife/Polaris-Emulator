package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.WiredCompatibilityDiagnostics;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredRoomDiagnostics;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.habbohotel.wired.core.WiredTextPlaceholderUtil;
import com.eu.habbo.habbohotel.wired.core.WiredVariableOperand;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Habbo's "write to logs" action ({@code wf_act_log}): writes its message into the room's wired log
 * at the level it was set to (0 debug, 1 info, 2 warning, 3 error), where the room log window lists
 * it. Username placeholders are filled in, and the users of its source are named after the line.
 *
 * <p>Int params: {@code [log level, user source]}. String param: the message, at most 400 characters.
 */
public class WiredEffectLog extends InteractionWiredEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectLog.class);
    public static final WiredEffectType type = WiredEffectType.WRITE_TO_LOGS;

    public static final int LEVEL_DEBUG = 0;
    public static final int LEVEL_INFO = 1;
    public static final int LEVEL_ERROR = 3;
    public static final int MAX_MESSAGE_LENGTH = 400;
    private static final int MAX_NAMED_USERS = 10;
    private static final int MAX_LINE_LENGTH = 1000;

    private String message = "";
    private int logLevel = LEVEL_INFO;
    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredEffectLog(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectLog(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.message);
        message.appendInt(2);
        message.appendInt(this.logLevel);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(type.code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            for (InteractionWiredTrigger object : room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY())) {
                if (!object.isTriggeredByRoomUnit()) {
                    invalidTriggers.add(object.getBaseItem().getSpriteId());
                }
            }
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) {
                message.appendInt(i);
            }
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        String message =
                (settings.getStringParam() != null) ? settings.getStringParam().trim() : "";
        if (message.isEmpty()) {
            return false;
        }
        this.message = (message.length() > MAX_MESSAGE_LENGTH) ? message.substring(0, MAX_MESSAGE_LENGTH) : message;

        int[] params = settings.getIntParams();
        this.logLevel = normalizeLevel((params.length > 0) ? params[0] : LEVEL_INFO);
        this.userSource = WiredVariableOperand.normalizeUserSource(
                (params.length > 1) ? params[1] : WiredSourceUtil.SOURCE_TRIGGER);

        this.setDelay(settings.getDelay());

        return true;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void execute(WiredContext ctx) {
        String line = WiredTextPlaceholderUtil.applyUsernamePlaceholders(ctx, this.message) + describeUsers(ctx);
        // Placeholders can grow the line; the log keeps a bounded one.
        if (line.length() > MAX_LINE_LENGTH) {
            line = line.substring(0, MAX_LINE_LENGTH);
        }

        WiredManager.noteWiredLog(
                ctx.room().getId(),
                WiredRoomDiagnostics.Severity.fromLogLevel(this.logLevel),
                line,
                this.getBaseItem().getName(),
                this.getId());
        LOGGER.debug("[WiredLog room {}] {}", ctx.room().getId(), line);
    }

    public int getLogLevel() {
        return this.logLevel;
    }

    static int normalizeLevel(int level) {
        return Math.clamp(level, LEVEL_DEBUG, LEVEL_ERROR);
    }

    /**
     * The dialog carries a user source and the log line never mentioned who. It names them now, so a
     * line written for "the triggering user" can be told apart from one written for the whole room.
     * Empty when the source resolves to nobody, which keeps a log line that never needed a user clean.
     */
    private String describeUsers(WiredContext ctx) {
        List<RoomUnit> users = WiredSourceUtil.resolveUsers(ctx, this.userSource);
        if (users.isEmpty()) {
            return "";
        }

        Room room = ctx.room();
        StringBuilder names = new StringBuilder();
        int named = 0;

        for (RoomUnit unit : users) {
            if (named++ >= MAX_NAMED_USERS) {
                names.append(" +").append(users.size() - MAX_NAMED_USERS);
                break;
            }

            Habbo habbo = (room != null) ? room.getHabbo(unit) : null;
            String name = (habbo != null && habbo.getHabboInfo() != null)
                    ? habbo.getHabboInfo().getUsername()
                    : ("#" + unit.getId());

            names.append(names.isEmpty() ? "" : ", ").append(name);
        }

        return " [" + names + "]";
    }

    @Override
    @Deprecated
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson()
                .toJson(new JsonData(this.message, this.getDelay(), this.userSource, this.logLevel));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.message = data.message;
            this.setDelay(data.delay);
            this.userSource = data.userSource;
            // Boxes saved before the level existed wrote info-like lines, so they read as info.
            this.logLevel = normalizeLevel((data.logLevel != null) ? data.logLevel : LEVEL_INFO);
        } else {
            String[] data = wiredData.split("\t");
            this.message = "";

            if (data.length >= 2) {
                super.setDelay(Integer.parseInt(data[0]));

                try {
                    this.message = data[1];
                } catch (Exception e) {
                    WiredCompatibilityDiagnostics.record(
                            WiredCompatibilityDiagnostics.FailurePoint.EFFECT_LOG_LEGACY,
                            this.getRoomId(),
                            this.getId(),
                            e);
                }
            }

            this.needsUpdate(true);
            this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        }
    }

    @Override
    public void onPickUp() {
        this.message = "";
        this.logLevel = LEVEL_INFO;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        this.setDelay(0);
    }

    @Override
    public boolean requiresTriggeringUser() {
        return false;
    }

    static class JsonData {
        String message;
        int delay;
        int userSource;
        Integer logLevel;

        public JsonData(String message, int delay, int userSource, int logLevel) {
            this.message = message;
            this.delay = delay;
            this.userSource = userSource;
            this.logLevel = logLevel;
        }
    }
}
