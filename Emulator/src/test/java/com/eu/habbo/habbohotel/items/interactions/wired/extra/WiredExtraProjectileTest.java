package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredProjectileDirections;
import com.eu.habbo.messages.ServerMessage;
import io.netty.buffer.ByteBuf;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class WiredExtraProjectileTest {

    private static Item base() {
        Item base = mock(Item.class);
        when(base.getType()).thenReturn(FurnitureType.FLOOR);
        when(base.getSpriteId()).thenReturn(5150);
        return base;
    }

    private static HabboItem furni(int id) {
        HabboItem item = mock(HabboItem.class);
        when(item.getId()).thenReturn(id);
        return item;
    }

    private static Room room(HabboItem... items) {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(81);
        for (HabboItem item : items) {
            when(room.getHabboItem(item.getId())).thenReturn(item);
        }
        return room;
    }

    private static WiredSettings settings(int[] params, int[] furniIds, Room room) {
        WiredSettings settings = new WiredSettings(params, "", furniIds, 0);
        settings.setRoom(room);
        return settings;
    }

    private static ResultSet row(String wiredData) throws SQLException {
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data")).thenReturn(wiredData);
        return set;
    }

    private static WiredExtraProjectile box() {
        return new WiredExtraProjectile(12, 1, base(), "", 0, 0);
    }

    private static final int[] PARAMS = {1, 3, 1, 1, 250, 2, 1, 1, 1, 75, 6, 5, 1, 1, 2, 1, -12, 3, 400};

    private record Body(int[] params, int code, int[] selected) {}

    private static Body body(WiredExtraProjectile box, Room room) {
        ServerMessage message = new ServerMessage(1);
        box.serializeWiredData(message, room);
        ByteBuf buffer = message.get();
        try {
            buffer.readInt();
            buffer.readShort();
            buffer.readByte();
            buffer.readInt();
            int[] selected = new int[buffer.readInt()];
            for (int i = 0; i < selected.length; i++) selected[i] = buffer.readInt();
            buffer.readInt();
            buffer.readInt();
            buffer.skipBytes(buffer.readShort());
            int[] params = new int[buffer.readInt()];
            for (int i = 0; i < params.length; i++) params[i] = buffer.readInt();
            buffer.readInt();
            return new Body(params, buffer.readInt(), selected);
        } finally {
            buffer.release();
        }
    }

    @Test
    void whatTheEditorSavesComesBackToItUnchanged() {
        HabboItem arrow = furni(300);
        Room room = room(arrow);
        WiredExtraProjectile box = box();

        assertTrue(box.saveData(settings(PARAMS, new int[] {300}, room), null));

        Body body = body(box, room);
        assertArrayEquals(PARAMS, body.params());
        assertArrayEquals(new int[] {300}, body.selected());
        assertEquals(136, body.code());
    }

    @Test
    void savedDataSurvivesTheDatabaseRoundTrip() throws Exception {
        HabboItem arrow = furni(300);
        Room room = room(arrow);
        WiredExtraProjectile saved = box();
        saved.saveData(settings(PARAMS, new int[] {300}, room), null);

        WiredExtraProjectile loaded = box();
        loaded.loadWiredData(row(saved.getWiredData()), room);

        assertArrayEquals(PARAMS, body(loaded, room).params());
        assertTrue(loaded.appliesTo(arrow));
    }

    @Test
    void outOfRangeParamsAreClampedAndMissingOnesDefault() {
        int[] clamped = WiredExtraProjectile.normalizeParams(new int[] {7, 9, 0, 0, 0, 0, 0, 0, 0, 0, 30, 999});

        assertEquals(1, clamped[WiredExtraProjectile.PARAM_ROTATE]);
        assertEquals(3, clamped[WiredExtraProjectile.PARAM_DIRECTIONAL_SYSTEM]);
        assertEquals(7, clamped[WiredExtraProjectile.PARAM_ROTATION_OFFSET]);
        assertEquals(127, clamped[11]);
        assertEquals(19, clamped.length);
        assertEquals(1, WiredExtraProjectile.normalizeParams(null)[WiredExtraProjectile.PARAM_ROTATE]);
    }

    @Test
    void aFreshBoxTurnsWhateverItsStackMoves() {
        WiredExtraProjectile box = box();

        assertTrue(box.appliesTo(furni(41)));
        assertEquals(WiredProjectileDirections.EAST, box.resolveRotation(2, 2, 6, 2, 4));
    }

    @Test
    void pickedFurniAreTheOnlyProjectiles() {
        HabboItem arrow = furni(300);
        Room room = room(arrow);
        WiredExtraProjectile box = box();
        box.saveData(settings(PARAMS, new int[] {300}, room), null);

        assertTrue(box.appliesTo(arrow));
        assertFalse(box.appliesTo(furni(301)));
        assertFalse(box.appliesTo(null));
    }

    @Test
    void theRotationFollowsTheSystemAndTheOffset() {
        Room room = room();
        WiredExtraProjectile box = box();
        int[] params = new int[19];
        params[WiredExtraProjectile.PARAM_ROTATE] = 1;
        params[WiredExtraProjectile.PARAM_DIRECTIONAL_SYSTEM] = WiredProjectileDirections.FOUR_PREFER_HORIZONTAL;
        params[WiredExtraProjectile.PARAM_ROTATION_OFFSET] = 4;
        box.saveData(settings(params, new int[0], room), null);

        assertEquals(WiredProjectileDirections.WEST, box.resolveRotation(0, 0, 3, 3, 2));
    }

    @Test
    void itLeavesTheRotationAloneWhenAskedToOrWhenNothingMoves() {
        Room room = room();
        WiredExtraProjectile off = box();
        int[] params = new int[19];
        off.saveData(settings(params, new int[0], room), null);

        assertEquals(4, off.resolveRotation(0, 0, 5, 0, 4));
        assertEquals(6, box().resolveRotation(3, 3, 3, 3, 6));
    }

    @Test
    void aSaveOutsideTheRoomIsRefused() {
        WiredExtraProjectile box = box();

        assertFalse(box.saveData(settings(PARAMS, new int[0], null), null));
    }
}
