package com.eu.habbo.habbohotel.wired.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTriggerOnFurni;
import com.eu.habbo.habbohotel.items.interactions.wired.triggers.WiredTriggerHabboPerformsAction;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredUserActionType;
import com.eu.habbo.habbohotel.wired.api.IWiredEffect;
import com.eu.habbo.habbohotel.wired.api.WiredStack;
import com.eu.habbo.habbohotel.wired.migrate.WiredEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

/** "User performs action: dance" + "triggerer is on furni" + an action, as a player builds it. */
class WiredDanceOnFurniStackTest {

    @Test
    void dancingOnThePickedFurniRunsTheAction() throws Exception {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(5);
        when(room.isLoaded()).thenReturn(true);

        HabboItem rug = mock(HabboItem.class);
        when(rug.getId()).thenReturn(900);
        when(rug.getRoomId()).thenReturn(5);
        when(room.getHabboItem(900)).thenReturn(rug);

        RoomTile tile = mock(RoomTile.class);
        when(room.getItemsAt(tile)).thenReturn(Set.of(rug));

        RoomUnit dancer = mock(RoomUnit.class);
        when(dancer.getId()).thenReturn(1);
        when(dancer.getRoomUnitType()).thenReturn(RoomUnitType.USER);
        when(dancer.getCurrentLocation()).thenReturn(tile);

        Item base = mock(Item.class);
        when(base.getType()).thenReturn(FurnitureType.FLOOR);

        WiredTriggerHabboPerformsAction trigger = new WiredTriggerHabboPerformsAction(1, 1, base, "", 0, 0);
        WiredConditionTriggerOnFurni condition = new WiredConditionTriggerOnFurni(2, 1, base, "", 0, 0);
        condition.setRoomId(5);

        GameEnvironment environment = mock(GameEnvironment.class, Answers.RETURNS_DEEP_STUBS);
        when(environment.getRoomManager().getRoom(5)).thenReturn(room);
        ConfigurationManager config = mock(ConfigurationManager.class);
        when(config.getInt(anyString())).thenReturn(5);
        when(config.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);
            emulator.when(Emulator::getConfig).thenReturn(config);

            // What the editors send: dance, no filters; the rug picked, user source "trigger".
            assertTrue(trigger.saveData(
                    new WiredSettings(new int[] {WiredUserActionType.DANCE, 0, 0, 0, 1}, "", new int[0], -1)));
            WiredSettings onRug = new WiredSettings(new int[] {0}, "", new int[] {900}, -1);
            onRug.setFurniSourceTypes(new int[] {WiredSourceUtil.SOURCE_SELECTED});
            onRug.setUserSourceTypes(new int[] {WiredSourceUtil.SOURCE_TRIGGER});
            assertTrue(condition.saveData(onRug));
        }

        List<String> calls = new ArrayList<>();
        IWiredEffect lamp = context -> calls.add("toggle");
        WiredStack stack = new WiredStack(mock(HabboItem.class), trigger, List.of(condition), List.of(lamp));
        WiredEngine engine = new WiredEngine(
                mock(WiredServices.class), (candidate, type) -> candidate == room ? List.of(stack) : List.of(), 100);

        engine.handleEvent(WiredEvents.userPerformsAction(room, dancer, WiredUserActionType.DANCE, 1));

        assertEquals(List.of("toggle"), calls);
    }
}
