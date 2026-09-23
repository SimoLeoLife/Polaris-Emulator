package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.installHotel;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.json;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredMatchFurniSetting;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredMoveCarryHelper;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Match furni to snapshot" takes its snapshot from the picked furni. With the dropdown left on
 * "triggering furni" the snapshot was recorded but applied to the trigger item instead.
 */
class WiredEffectMatchFurniTest {

    @Test
    void aSnapshotOfPickedFurniPromotesTheTriggerSourceToSelected() throws Exception {
        Room room = mock(Room.class);
        HabboItem item = mock(HabboItem.class);
        when(item.getId()).thenReturn(301);
        when(item.allowWiredResetState()).thenReturn(true);
        when(item.getExtradata()).thenReturn("1");
        when(room.getHabboItem(301)).thenReturn(item);
        WiredEffectMatchFurni box = new WiredEffectMatchFurni(1, 1, base(), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            installHotel(emulator, room);

            box.saveData(
                    new WiredSettings(new int[] {1, 0, 0, 0, WiredSourceUtil.SOURCE_TRIGGER}, "", new int[] {301}, 0),
                    null);
        }

        assertEquals(
                WiredSourceUtil.SOURCE_SELECTED, json(box).get("furniSource").getAsInt());
        assertEquals(1, json(box).getAsJsonArray("items").size());
    }

    @Test
    void directionAndAltitudeWithoutPositionRestoreBoth() throws Exception {
        Room room = mock(Room.class);
        RoomLayout layout = mock(RoomLayout.class);
        RoomTile tile = mock(RoomTile.class);
        when(room.getLayout()).thenReturn(layout);
        when(layout.getTile((short) 3, (short) 4)).thenReturn(tile);
        HabboItem item = mock(HabboItem.class);
        when(item.getX()).thenReturn((short) 3);
        when(item.getY()).thenReturn((short) 4);
        when(item.getZ()).thenReturn(0.0);
        when(item.getRotation()).thenReturn(0);
        WiredEffectMatchFurni box = new WiredEffectMatchFurni(1, 1, base(), "", 0, 0);
        box.loadWiredData(
                row("{\"state\":false,\"direction\":true,\"position\":false,\"altitude\":true,"
                        + "\"items\":[],\"delay\":0,\"furniSource\":0}"),
                room);
        WiredContext ctx = context(room);

        try (MockedStatic<WiredMoveCarryHelper> moves = mockStatic(WiredMoveCarryHelper.class)) {
            box.applySetting(room, item, new WiredMatchFurniSetting(301, " ", 2, 3, 4, 1.5), ctx);

            // The direction branch used to win and the height never came back.
            moves.verify(() -> WiredMoveCarryHelper.moveFurni(
                    eq(room), eq(box), eq(item), eq(tile), eq(2), eq(1.5), isNull(), eq(true), eq(ctx)));
        }
    }

    @Test
    void aShortLegacyRowKeepsTheDefaults() throws Exception {
        WiredEffectMatchFurni box = new WiredEffectMatchFurni(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("5"), null);

        assertEquals(
                WiredSourceUtil.SOURCE_TRIGGER, json(box).get("furniSource").getAsInt());
        assertEquals(0, json(box).getAsJsonArray("items").size());
    }
}
