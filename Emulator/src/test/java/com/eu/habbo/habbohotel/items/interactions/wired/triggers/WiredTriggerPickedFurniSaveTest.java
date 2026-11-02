package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.users.HabboItem;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

/**
 * The official editor has no furni source on a trigger: picked furni mean the picks. A trigger that
 * gets the "trigger" source (0) next to picks keeps them instead of dropping them.
 */
class WiredTriggerPickedFurniSaveTest {

    @Test
    void walkOnFurniKeepsItsPicksWhenTheSourceSaysTrigger() throws Exception {
        Room room = WiredTriggerTestSupport.room(7);
        HabboItem rug = mock(HabboItem.class);
        when(rug.getId()).thenReturn(1234);
        when(room.getHabboItem(1234)).thenReturn(rug);

        GameEnvironment environment = mock(GameEnvironment.class, Answers.RETURNS_DEEP_STUBS);
        RoomManager rooms = mock(RoomManager.class);
        when(environment.getRoomManager()).thenReturn(rooms);
        when(rooms.getRoom(7)).thenReturn(room);

        WiredTriggerHabboWalkOnFurni trigger =
                new WiredTriggerHabboWalkOnFurni(1, 1, WiredTriggerTestSupport.boxBase(), "", 0, 0);
        trigger.setRoomId(7);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);

            assertTrue(trigger.saveData(new WiredSettings(new int[] {0}, "", new int[] {1234}, -1)));
        }

        String saved = trigger.getWiredData();
        assertTrue(saved.contains("1234"), saved);
        assertTrue(saved.contains("100"), saved);

        // Walking onto the picked rug fires it; walking onto another furni does not.
        HabboItem chair = mock(HabboItem.class);
        when(chair.getId()).thenReturn(99);
        when(room.getItemsAt(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(java.util.Set.of());
        assertTrue(trigger.matches(trigger, walkOn(room, rug)));
        org.junit.jupiter.api.Assertions.assertFalse(trigger.matches(trigger, walkOn(room, chair)));
    }

    private static com.eu.habbo.habbohotel.wired.core.WiredEvent walkOn(Room room, HabboItem item) {
        return com.eu.habbo.habbohotel.wired.core.WiredEvent.builder(
                        com.eu.habbo.habbohotel.wired.core.WiredEvent.Type.USER_WALKS_ON, room)
                .sourceItem(item)
                .build();
    }
}
