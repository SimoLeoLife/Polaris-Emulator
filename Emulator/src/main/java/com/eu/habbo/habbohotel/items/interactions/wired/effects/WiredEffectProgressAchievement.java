package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.WiredPlatform;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredRewardPolicy;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraAchievementEnabler;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEngine;
import com.eu.habbo.habbohotel.wired.core.WiredHotelProgress;
import com.eu.habbo.habbohotel.wired.core.WiredHotelProgressPolicy;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredProgressLimiter;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.habbohotel.wired.core.WiredVariableOperand;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Habbo's progress-achievement action: progresses one of the room's achievements for the selected
 * users, by a fixed amount ({@code mode 1}) or up to it ({@code mode 0}).
 *
 * <p>Achievements are hotel-wide, so this only works when the hotel turns it on
 * ({@code hotel.wired.achievements.enabled}), for achievements on its allow-list that an achievement
 * enabler in the same room also names, for users still in the room, and within each user's
 * allowance per window. Saving it needs the value-granting wired permission.
 *
 * <p>String param: the achievement name. Int params: {@code [mode, amount, user source]}.
 */
public class WiredEffectProgressAchievement extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.PROGRESS_ACHIEVEMENT;
    public static final int MAX_NAME_LENGTH = WiredExtraAchievementEnabler.MAX_NAME_LENGTH;

    private String achievement = "";
    private int mode = WiredHotelProgress.MODE_ADD;
    private int amount = 1;
    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredEffectProgressAchievement(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectProgressAchievement(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.achievement);
        message.appendInt(3);
        message.appendInt(this.mode);
        message.appendInt(this.amount);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(type.code);
        message.appendInt(this.getDelay());
        WiredEffectProgressRewardTrack.appendInvalidTriggers(this, message, room);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        if (!WiredRewardPolicy.canConfigure(gameClient)) {
            return false;
        }

        String name = normalizeName(settings.getStringParam());
        if (name.isEmpty()) {
            return false;
        }

        int[] params = settings.getIntParams();
        this.achievement = name;
        this.mode = WiredHotelProgress.normalizeMode(params.length > 0 ? params[0] : WiredHotelProgress.MODE_ADD);
        this.amount = WiredHotelProgress.normalizeAmount(params.length > 1 ? params[1] : 1);
        this.userSource = WiredVariableOperand.normalizeUserSource(
                params.length > 2 ? params[2] : WiredSourceUtil.SOURCE_TRIGGER);
        this.setDelay(settings.getDelay());
        return true;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        GameEnvironment environment = WiredPlatform.gameEnvironment();
        WiredEngine engine = WiredManager.getEngine();
        WiredHotelProgressPolicy policy = WiredHotelProgressPolicy.achievements(WiredPlatform.configuration());
        if (room == null || environment == null || engine == null || !policy.allows(this.achievement)) {
            return;
        }

        Set<String> declared = WiredExtraAchievementEnabler.declaredAchievements(room);
        if (!declared.contains(this.achievement)) {
            return;
        }

        AchievementManager manager = environment.getAchievementManager();
        Achievement target = manager == null ? null : manager.getAchievement(this.achievement);
        List<Habbo> users = WiredHotelProgress.presentUsers(room, WiredSourceUtil.resolveUsers(ctx, this.userSource));
        if (target == null || users.isEmpty()) {
            return;
        }

        int mode = this.mode;
        int amount = this.amount;
        WiredProgressLimiter limiter = engine.progressLimiter();
        WiredHotelProgress.dispatch(() -> WiredHotelProgress.progressAchievement(
                room, users, target, declared, mode, amount, policy, limiter, AchievementManager::progressAchievement));
    }

    @Override
    @Deprecated
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson()
                .toJson(new JsonData(this.achievement, this.mode, this.amount, this.getDelay(), this.userSource));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.onPickUp();

        String wiredData = set.getString("wired_data");
        if (wiredData == null || !wiredData.startsWith("{")) {
            return;
        }

        JsonData data;
        try {
            data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
        } catch (RuntimeException e) {
            return;
        }
        if (data == null) {
            return;
        }

        this.achievement = normalizeName(data.achievement);
        this.mode = WiredHotelProgress.normalizeMode(data.mode);
        this.amount = WiredHotelProgress.normalizeAmount(data.amount);
        this.userSource = WiredVariableOperand.normalizeUserSource(data.userSource);
        this.setDelay(data.delay);
    }

    @Override
    public void onPickUp() {
        this.achievement = "";
        this.mode = WiredHotelProgress.MODE_ADD;
        this.amount = 1;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        this.setDelay(0);
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredSourceUtil.SOURCE_TRIGGER;
    }

    public String getAchievement() {
        return this.achievement;
    }

    public int getMode() {
        return this.mode;
    }

    public int getAmount() {
        return this.amount;
    }

    public int getUserSource() {
        return this.userSource;
    }

    /** One name as the enabler accepts it, or empty. */
    static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        List<String> names = WiredExtraAchievementEnabler.parseNames(value.trim());
        return names.size() == 1 && names.get(0).length() <= MAX_NAME_LENGTH ? names.get(0) : "";
    }

    static class JsonData {
        String achievement;
        int mode;
        int amount;
        int delay;
        int userSource;

        JsonData(String achievement, int mode, int amount, int delay, int userSource) {
            this.achievement = achievement;
            this.mode = mode;
            this.amount = amount;
            this.delay = delay;
            this.userSource = userSource;
        }
    }
}
