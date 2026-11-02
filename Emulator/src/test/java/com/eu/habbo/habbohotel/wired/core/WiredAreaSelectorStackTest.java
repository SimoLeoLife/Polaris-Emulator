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
import com.eu.habbo.habbohotel.items.interactions.wired.selector.WiredEffectFurniArea;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.api.IWiredEffect;
import com.eu.habbo.habbohotel.wired.api.IWiredTrigger;
import com.eu.habbo.habbohotel.wired.api.WiredStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

/** "Says something" + "furni in area" selector + an action whose furni source is the selector. */
class WiredAreaSelectorStackTest {

    @Test
    void anActionOnTheSelectorSourceGetsTheFurniInTheArea() throws Exception {
        Room room = mock(Room.class, Answers.RETURNS_DEEP_STUBS);
        when(room.getId()).thenReturn(5);
        when(room.isLoaded()).thenReturn(true);

        HabboItem inside = mock(HabboItem.class);
        when(inside.getId()).thenReturn(901);
        when(inside.getRoomId()).thenReturn(5);
        HabboItem outside = mock(HabboItem.class);
        when(outside.getId()).thenReturn(902);
        when(outside.getRoomId()).thenReturn(5);
        when(room.getItemsAt(anyInt(), anyInt())).thenReturn(Set.of());
        when(room.getItemsAt(2, 2)).thenReturn(Set.of(inside));
        when(room.getItemsAt(8, 8)).thenReturn(Set.of(outside));
        when(room.getFloorItems()).thenReturn(Set.of(inside, outside));

        Item base = mock(Item.class);
        when(base.getType()).thenReturn(FurnitureType.FLOOR);
        WiredEffectFurniArea area = new WiredEffectFurniArea(3, 1, base, "", 0, 0);
        area.setRoomId(5);

        GameEnvironment environment = mock(GameEnvironment.class, Answers.RETURNS_DEEP_STUBS);
        when(environment.getRoomManager().getRoom(5)).thenReturn(room);
        ConfigurationManager config = mock(ConfigurationManager.class);
        when(config.getInt(anyString())).thenReturn(5);
        when(config.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));

        List<Integer> toggled = new ArrayList<>();
        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);
            emulator.when(Emulator::getConfig).thenReturn(config);

            assertTrue(area.saveData(new WiredSettings(new int[] {1, 1, 3, 3, 0, 0}, "", new int[0], -1), null));

            IWiredTrigger says = new IWiredTrigger() {
                @Override
                public WiredEvent.Type listensTo() {
                    return WiredEvent.Type.CUSTOM;
                }

                @Override
                public boolean matches(HabboItem triggerItem, WiredEvent event) {
                    return true;
                }
            };
            IWiredEffect toggle =
                    context -> WiredSourceUtil.resolveItems(context, WiredSourceUtil.SOURCE_SELECTOR, List.of())
                            .forEach(item -> toggled.add(item.getId()));
            WiredStack stack = new WiredStack(mock(HabboItem.class), says, List.of(), List.of(area, toggle));
            WiredEngine engine = new WiredEngine(
                    mock(WiredServices.class),
                    (candidate, type) -> candidate == room ? List.of(stack) : List.of(),
                    100);

            engine.handleEvent(WiredEvent.builder(WiredEvent.Type.CUSTOM, room)
                    .createdAtMs(System.currentTimeMillis())
                    .build());
        }

        assertEquals(List.of(901), toggled);
    }
}
