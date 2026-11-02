package com.eu.habbo.habbohotel.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionTeleport;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import org.junit.jupiter.api.Test;

/** A furni that sent someone to another room decides how they arrive there, once. */
class PendingRoomEntrySupportTest {

    private static final long NOW = 100_000L;

    private static HabboInfo info() {
        HabboInfo info = mock(HabboInfo.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        info.setRoomEntryMethod(PendingRoomEntry.METHOD_DOOR);
        return info;
    }

    private static Habbo habbo(HabboInfo info, RoomUnit unit) {
        Habbo habbo = mock(Habbo.class);
        when(habbo.getHabboInfo()).thenReturn(info);
        when(habbo.getRoomUnit()).thenReturn(unit);
        return habbo;
    }

    private static Room room(int id) {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(id);
        return room;
    }

    @Test
    void aRoomLinkMarksTheEntryAsARoomNetwork() {
        HabboInfo info = info();
        info.setPendingRoomEntry(PendingRoomEntry.roomNetwork(55, NOW));

        PendingRoomEntrySupport.apply(habbo(info, mock(RoomUnit.class)), room(55), true, NOW + 1000);

        assertEquals(PendingRoomEntry.METHOD_ROOM_NETWORK, info.getRoomEntryMethod());
        assertNull(info.takePendingRoomEntry(55, NOW + 1000));
    }

    @Test
    void aTeleporterPutsThemOnItsPairFacingItsWay() {
        Room room = room(66);
        RoomLayout layout = mock(RoomLayout.class);
        RoomTile tile = mock(RoomTile.class);
        when(tile.getStackHeight()).thenReturn(1.5);
        when(room.getLayout()).thenReturn(layout);
        when(layout.getTile((short) 3, (short) 4)).thenReturn(tile);
        Item base = mock(Item.class);
        InteractionTeleport pair = new InteractionTeleport(501, 1, base, "", 0, 0);
        pair.setX((short) 3);
        pair.setY((short) 4);
        pair.setRotation(2);
        when(room.getHabboItem(501)).thenReturn(pair);
        RoomUnit unit = mock(RoomUnit.class);
        HabboInfo info = info();
        info.setPendingRoomEntry(PendingRoomEntry.teleport(66, 501, NOW));

        PendingRoomEntrySupport.apply(habbo(info, unit), room, true, NOW);

        verify(unit).setLocation(tile);
        verify(unit).setZ(1.5);
        verify(unit).setBodyRotation(RoomUserRotation.EAST);
        assertEquals(PendingRoomEntry.METHOD_TELEPORT, info.getRoomEntryMethod());
        assertEquals(501, info.getRoomEntryTeleportId());
    }

    @Test
    void aStaleOrForeignOrNonDoorEntryChangesNothingAndIsDropped() {
        RoomUnit unit = mock(RoomUnit.class);
        HabboInfo info = info();

        info.setPendingRoomEntry(PendingRoomEntry.roomNetwork(55, NOW));
        PendingRoomEntrySupport.apply(habbo(info, unit), room(56), true, NOW);
        assertEquals(PendingRoomEntry.METHOD_DOOR, info.getRoomEntryMethod());

        info.setPendingRoomEntry(PendingRoomEntry.roomNetwork(55, NOW));
        PendingRoomEntrySupport.apply(habbo(info, unit), room(55), true, NOW + PendingRoomEntry.LIFETIME_MS + 1);
        assertEquals(PendingRoomEntry.METHOD_DOOR, info.getRoomEntryMethod());

        info.setPendingRoomEntry(PendingRoomEntry.teleport(55, 7, NOW));
        PendingRoomEntrySupport.apply(habbo(info, unit), room(55), false, NOW);
        assertEquals(PendingRoomEntry.METHOD_DOOR, info.getRoomEntryMethod());
        assertNull(info.takePendingRoomEntry(55, NOW));
        verify(unit, never()).setLocation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aTeleporterThatIsGoneLeavesThemAtTheDoor() {
        RoomUnit unit = mock(RoomUnit.class);
        HabboInfo info = info();
        info.setPendingRoomEntry(PendingRoomEntry.teleport(66, 501, NOW));

        PendingRoomEntrySupport.apply(habbo(info, unit), room(66), true, NOW);

        verify(unit, never()).setLocation(org.mockito.ArgumentMatchers.any());
        assertEquals(PendingRoomEntry.METHOD_DOOR, info.getRoomEntryMethod());
    }

    @Test
    void oneFurniForwardPerIntervalForEachUser() {
        HabboInfo info = info();

        assertTrue(info.tryAcquireRoomForward(NOW, 2000));
        assertFalse(info.tryAcquireRoomForward(NOW + 1999, 2000));
        assertTrue(info.tryAcquireRoomForward(NOW + 2000, 2000));
    }
}
