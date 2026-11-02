package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.ItemManager;
import com.eu.habbo.habbohotel.rooms.BuildersClubRoomSupport;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredServices;
import com.eu.habbo.habbohotel.wired.core.WiredState;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Habbo's remove furni never destroys anything. Even the old DELETE mode hands every furni back to
 * whoever owns it, so a wired builder with modify rights cannot wipe the room owner's furni for good.
 */
class WiredEffectRemoveFurniTest {

    @Test
    void deleteModeReturnsFurniToTheirOwnersInsteadOfDestroyingThem() throws Exception {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(1);
        when(room.getOwnerId()).thenReturn(10);
        HabboItem owners = furni(room, 301, 10);
        HabboItem guests = furni(room, 302, 20);

        WiredEffectRemoveFurni box = new WiredEffectRemoveFurni(1, 10, mock(Item.class), "", 0, 0);
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data"))
                .thenReturn("{\"items\":[301,302],\"mode\":1,\"furniSource\":100,\"delay\":0}");
        box.loadWiredData(set, room);

        ItemManager items = mock(ItemManager.class);
        GameEnvironment environment = mock(GameEnvironment.class);
        when(environment.getItemManager()).thenReturn(items);
        when(environment.getHabboManager()).thenReturn(mock(HabboManager.class));

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class);
                MockedStatic<BuildersClubRoomSupport> buildersClub = mockStatic(BuildersClubRoomSupport.class)) {
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);
            buildersClub
                    .when(() -> BuildersClubRoomSupport.isTrackedItem(anyInt()))
                    .thenReturn(false);

            box.execute(context(room));
        }

        verify(room).pickUpItem(owners, null);
        verify(room).pickUpItem(guests, null);
        verify(items, never()).deleteItem(any());
    }

    private static HabboItem furni(Room room, int id, int userId) {
        HabboItem item = mock(HabboItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getUserId()).thenReturn(userId);
        when(item.getRoomId()).thenReturn(1);
        when(room.getHabboItem(id)).thenReturn(item);
        return item;
    }

    private static WiredContext context(Room room) {
        return new WiredContext(
                WiredEvent.builder(WiredEvent.Type.CUSTOM, room).build(),
                null,
                mock(WiredServices.class),
                new WiredState(20));
    }
}
