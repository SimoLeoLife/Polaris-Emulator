package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

/**
 * A toggle box keeps its picked furni whatever its furni source, as the official editor does: with a
 * selector as the source the picks are unused, but saving must not throw them away.
 */
class WiredEffectToggleFurniPicksTest {

    @Test
    void picksSurviveASaveWithTheSelectorAsSource() throws Exception {
        Room room = mock(Room.class);
        HabboItem lamp = mock(HabboItem.class);
        when(lamp.getId()).thenReturn(1234);
        when(room.getHabboItem(1234)).thenReturn(lamp);

        GameEnvironment environment = mock(GameEnvironment.class, Answers.RETURNS_DEEP_STUBS);
        RoomManager rooms = mock(RoomManager.class);
        when(environment.getRoomManager()).thenReturn(rooms);
        when(rooms.getRoom(7)).thenReturn(room);
        ConfigurationManager config = mock(ConfigurationManager.class);
        when(config.getInt(anyString())).thenReturn(20);
        when(config.getInt(anyString(), anyInt())).thenReturn(120);

        Item base = mock(Item.class);
        WiredEffectToggleFurni toggle = new WiredEffectToggleFurni(1, 1, base, "", 0, 0);
        toggle.setRoomId(7);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);
            emulator.when(Emulator::getConfig).thenReturn(config);

            // Official layout: [toggle type], the furni source in the official source slot.
            WiredSettings settings = new WiredSettings(new int[] {0}, "", new int[] {1234}, -1);
            settings.setFurniSourceTypes(new int[] {WiredSourceUtil.SOURCE_SELECTOR});
            assertTrue(toggle.saveData(settings, null));
        }

        String saved = toggle.getWiredData();
        assertTrue(saved.contains("1234"), saved);
        assertTrue(saved.contains("\"furniSource\":" + WiredSourceUtil.SOURCE_SELECTOR), saved);
    }
}
