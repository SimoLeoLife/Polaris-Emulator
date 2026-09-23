package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.WiredPlatform;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraAchievementEnabler;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboStats;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEngine;
import com.eu.habbo.habbohotel.wired.core.WiredHotelProgress;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredProgressLimiter;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.outgoing.wired.WiredEffectDataComposer;
import com.eu.habbo.threading.ThreadPooling;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Habbo's progress-achievement action: saved only by staff, fired only through every hotel gate. */
class WiredEffectProgressAchievementTest {

    private static WiredEffectProgressAchievement box() {
        return new WiredEffectProgressAchievement(9, 1, base(), "", 0, 0);
    }

    private static WiredSettings settings(String name, int... params) {
        return new WiredSettings(params, name, new int[0], 0);
    }

    static GameClient author(boolean superwired) {
        GameClient client = mock(GameClient.class);
        Habbo habbo = mock(Habbo.class);
        when(client.getHabbo()).thenReturn(habbo);
        when(habbo.hasPermission(Permission.ACC_SUPERWIRED)).thenReturn(superwired);
        return client;
    }

    record Body(String text, int[] params, int code) {}

    static Body body(com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect box) {
        Room room = mock(Room.class);
        RoomSpecialTypes specialTypes = mock(RoomSpecialTypes.class);
        when(specialTypes.getTriggers(anyInt(), anyInt())).thenReturn(Set.of());
        when(room.getRoomSpecialTypes()).thenReturn(specialTypes);

        ByteBuf packet = new WiredEffectDataComposer(box, room).compose().get();
        try {
            packet.skipBytes(6);
            packet.readBoolean();
            packet.readInt();
            packet.readInt();
            packet.readInt();
            packet.readInt();
            byte[] text = new byte[packet.readShort()];
            packet.readBytes(text);
            int[] params = new int[packet.readInt()];
            for (int i = 0; i < params.length; i++) params[i] = packet.readInt();
            packet.readInt();
            return new Body(new String(text, StandardCharsets.UTF_8), params, packet.readInt());
        } finally {
            packet.release();
        }
    }

    @Test
    void onlyAStaffAuthorMaySaveIt() {
        WiredEffectProgressAchievement box = box();

        assertFalse(box.saveData(settings("ACH_Game", 1, 5, 0), author(false)));
        assertFalse(box.saveData(settings("ACH_Game", 1, 5, 0), null));
        assertEquals("", box.getAchievement());
        assertTrue(box.saveData(settings("ACH_Game", 1, 5, 0), author(true)));
        assertEquals("ACH_Game", box.getAchievement());
    }

    @Test
    void theParamsAreValidatedAndClamped() {
        WiredEffectProgressAchievement box = box();

        assertFalse(box.saveData(settings("", 1, 5, 0), author(true)), "a name is required");
        assertFalse(box.saveData(settings("two names", 1, 5, 0), author(true)), "one name only");
        assertFalse(box.saveData(settings("x".repeat(65), 1, 5, 0), author(true)));

        assertTrue(box.saveData(settings(" ACH_Game ", 7, Integer.MAX_VALUE, 999), author(true)));
        assertEquals(WiredHotelProgress.MODE_ADD, box.getMode());
        assertEquals(WiredHotelProgress.MAX_AMOUNT, box.getAmount());
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, box.getUserSource(), "an unknown user source falls back");

        assertTrue(box.saveData(settings("ACH_Game", 0, -3), author(true)));
        assertEquals(WiredHotelProgress.MODE_RAISE_TO, box.getMode());
        assertEquals(1, box.getAmount());
    }

    @Test
    void theDialogGetsItsParamsUnderItsOwnCode() {
        WiredEffectProgressAchievement box = box();
        box.saveData(settings("ACH_Game", 0, 25, WiredSourceUtil.SOURCE_SELECTOR), author(true));

        Body body = body(box);
        assertEquals("ACH_Game", body.text());
        assertArrayEquals(new int[] {0, 25, WiredSourceUtil.SOURCE_SELECTOR}, body.params());
        assertEquals(WiredEffectType.PROGRESS_ACHIEVEMENT.code, body.code());
    }

    @Test
    void theSettingsSurviveTheDatabaseAndTamperedRowsAreClamped() throws Exception {
        WiredEffectProgressAchievement saved = box();
        saved.saveData(settings("ACH_Game", 0, 25, WiredSourceUtil.SOURCE_SELECTOR), author(true));

        WiredEffectProgressAchievement loaded = box();
        loaded.loadWiredData(row(saved.getWiredData()), null);
        assertEquals("ACH_Game", loaded.getAchievement());
        assertEquals(0, loaded.getMode());
        assertEquals(25, loaded.getAmount());
        assertEquals(WiredSourceUtil.SOURCE_SELECTOR, loaded.getUserSource());

        WiredEffectProgressAchievement tampered = box();
        tampered.loadWiredData(
                row("{\"achievement\":\"a b\",\"mode\":5,\"amount\":99999999,\"delay\":0,\"userSource\":77}"), null);
        assertEquals("", tampered.getAchievement());
        assertEquals(WiredHotelProgress.MAX_AMOUNT, tampered.getAmount());
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, tampered.getUserSource());

        WiredEffectProgressAchievement broken = box();
        broken.loadWiredData(row("{oops"), null);
        assertEquals("", broken.getAchievement());
    }

    // ---------------------------------------------------------------- firing

    private static Achievement achievement() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getInt("id")).thenReturn(3);
        when(row.getString("name")).thenReturn("ACH_Game");
        when(row.getString("category")).thenReturn("games");
        when(row.findColumn("state")).thenThrow(new java.sql.SQLException("legacy"));
        return new Achievement(row);
    }

    private static ConfigurationManager config(boolean enabled, String allowed) {
        ConfigurationManager config = mock(ConfigurationManager.class);
        when(config.getBoolean("hotel.wired.achievements.enabled", false)).thenReturn(enabled);
        when(config.getValue("hotel.wired.achievements.allowed", "")).thenReturn(allowed);
        when(config.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));
        return config;
    }

    /** Fires the box once in a room with a user in it and checks how often the achievement moved. */
    private static void fire(ConfigurationManager config, boolean declared, int expected) throws Exception {
        WiredEffectProgressAchievement box = box();
        box.saveData(settings("ACH_Game", 1, 2, WiredSourceUtil.SOURCE_TRIGGER), author(true));

        WiredExtraAchievementEnabler enabler = new WiredExtraAchievementEnabler(10, 1, base(), "", 0, 0);
        enabler.saveData(
                new WiredSettings(new int[0], declared ? "ACH_Game" : "ACH_Other", new int[0], 0), author(true));

        Room room = mock(Room.class);
        RoomSpecialTypes specialTypes = mock(RoomSpecialTypes.class);
        when(specialTypes.getExtras()).thenReturn(new LinkedHashSet<InteractionWiredExtra>(List.of(enabler)));
        when(room.getRoomSpecialTypes()).thenReturn(specialTypes);

        RoomUnit unit = mock(RoomUnit.class);
        when(unit.isInRoom()).thenReturn(true);
        Habbo habbo = mock(Habbo.class);
        HabboInfo info = mock(HabboInfo.class);
        when(info.getId()).thenReturn(7);
        when(info.getCurrentRoom()).thenReturn(room);
        when(habbo.getHabboInfo()).thenReturn(info);
        when(habbo.getRoomUnit()).thenReturn(unit);
        when(habbo.getHabboStats()).thenReturn(mock(HabboStats.class));
        when(room.getHabbo(unit)).thenReturn(habbo);

        Achievement achievement = achievement();
        AchievementManager achievements = mock(AchievementManager.class);
        when(achievements.getAchievement("ACH_Game")).thenReturn(achievement);
        GameEnvironment environment = mock(GameEnvironment.class);
        when(environment.getAchievementManager()).thenReturn(achievements);
        ThreadPooling threading = mock(ThreadPooling.class);
        when(threading.run(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        WiredEngine engine = mock(WiredEngine.class);
        when(engine.progressLimiter()).thenReturn(new WiredProgressLimiter());

        WiredContext ctx = WiredEffectTestFixtures.context(room, unit);
        try (MockedStatic<WiredPlatform> platform = Mockito.mockStatic(WiredPlatform.class);
                MockedStatic<WiredManager> manager =
                        Mockito.mockStatic(WiredManager.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<AchievementManager> progress = Mockito.mockStatic(AchievementManager.class)) {
            platform.when(WiredPlatform::configuration).thenReturn(config);
            platform.when(WiredPlatform::gameEnvironment).thenReturn(environment);
            platform.when(WiredPlatform::threading).thenReturn(threading);
            manager.when(WiredManager::getEngine).thenReturn(engine);

            box.execute(ctx);

            progress.verify(
                    () -> AchievementManager.progressAchievement(habbo, achievement, 2), Mockito.times(expected));
            Mockito.verify(threading, Mockito.times(expected)).run(any(Runnable.class));
        }
    }

    @Test
    void aFiringProgressesTheUserOnlyWhenEveryGateIsOpen() throws Exception {
        fire(config(true, "ACH_Game"), true, 1);
    }

    @Test
    void aFiringDoesNothingWhileTheHotelSwitchIsOff() throws Exception {
        fire(config(false, "ACH_Game"), true, 0);
        fire(null, true, 0);
    }

    @Test
    void aFiringDoesNothingForAnAchievementOffTheAllowList() throws Exception {
        fire(config(true, "ACH_Other"), true, 0);
    }

    @Test
    void aFiringDoesNothingWithoutAnEnablerNamingIt() throws Exception {
        fire(config(true, "ACH_Game"), false, 0);
    }
}
