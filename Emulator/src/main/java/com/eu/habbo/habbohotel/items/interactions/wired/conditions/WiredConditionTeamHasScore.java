package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.GameTeam;
import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionTeamHasScore extends WiredConditionTeamGameBase {
    public static final WiredConditionType type = WiredConditionType.TEAM_HAS_SCORE;

    private int teamType = GameTeamColors.RED.type;
    private int comparison = COMPARISON_EQUAL;
    private int score = 0;
    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;
    private int quantifier = QUANTIFIER_ALL;

    public WiredConditionTeamHasScore(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionTeamHasScore(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean evaluate(WiredContext ctx) {
        // The score of the named team in the room's game, as Habbo asks it: whoever set the stack
        // off, and also when nobody did (a timer, a game ending). Before, the box went through the
        // users and failed without one, or for anyone who was not in that team.
        Game game = this.resolveActiveRoomGame(ctx.room());
        if (game == null) {
            return false;
        }

        GameTeam team = game.getTeam(this.resolveConfiguredTeamColor(this.teamType));
        int score = (team != null) ? team.getTotalScore() : 0;

        return this.compareValue(score, this.score, this.comparison);
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson()
                .toJson(new JsonData(this.teamType, this.comparison, this.score, this.userSource, this.quantifier));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.resetSettings();

        String wiredData = set.getString("wired_data");
        if (wiredData == null || !wiredData.startsWith("{")) {
            return;
        }

        JsonData data;
        try {
            data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
        } catch (RuntimeException ignored) {
            this.resetSettings();
            return;
        }
        if (data == null) {
            return;
        }

        this.teamType = this.normalizeExplicitTeamType(data.teamType);
        this.comparison = this.normalizeComparison(data.comparison);
        this.score = this.normalizeScore(data.score);
        this.userSource = this.normalizeUserSource(data.userSource);
        this.quantifier = this.normalizeQuantifier(data.quantifier);
    }

    @Override
    public void onPickUp() {
        this.resetSettings();
    }

    @Override
    public WiredConditionType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(5);
        message.appendInt(this.teamType);
        message.appendInt(this.comparison);
        message.appendInt(this.score);
        message.appendInt(this.userSource);
        message.appendInt(this.quantifier);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        int[] params = settings.getIntParams();
        this.resetSettings();

        if (params.length > 0) this.teamType = this.normalizeExplicitTeamType(params[0]);
        if (params.length > 1) this.comparison = this.normalizeComparison(params[1]);
        if (params.length > 2) this.score = this.normalizeScore(params[2]);
        if (params.length > 3) this.userSource = this.normalizeUserSource(params[3]);
        if (params.length > 4) this.quantifier = this.normalizeQuantifier(params[4]);

        return true;
    }

    private void resetSettings() {
        this.teamType = GameTeamColors.RED.type;
        this.comparison = COMPARISON_EQUAL;
        this.score = 0;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        this.quantifier = QUANTIFIER_ALL;
    }

    static class JsonData {
        int teamType;
        int comparison;
        int score;
        int userSource;
        int quantifier;

        public JsonData(int teamType, int comparison, int score, int userSource, int quantifier) {
            this.teamType = teamType;
            this.comparison = comparison;
            this.score = score;
            this.userSource = userSource;
            this.quantifier = quantifier;
        }
    }
}
