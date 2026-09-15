package com.eu.habbo.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.GuildRank;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.incoming.floorplaneditor.FloorPlanEditorRequestDoorSettingsEvent;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.messages.outgoing.guilds.GuildMemberUpdateComposer;
import com.eu.habbo.messages.outgoing.guilds.GuildRefreshMembersListComposer;
import com.eu.habbo.messages.outgoing.rooms.pets.breeding.PetBreedingStartFailedComposer;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PacketReceiverCompatibilityTest {
    @Test
    void floorEditorReceivesCurrentThicknessWithoutChangingLegacyResponse() throws Exception {
        GameClient client = clientInRoom(mock(Room.class));
        Room room = client.getHabbo().getHabboInfo().getCurrentRoom();
        when(room.isHideWall()).thenReturn(true);
        when(room.getWallSize()).thenReturn(-2);
        when(room.getFloorSize()).thenReturn(1);

        FloorPlanEditorRequestDoorSettingsEvent event = new FloorPlanEditorRequestDoorSettingsEvent();
        event.client = client;
        event.handle();

        ArgumentCaptor<MessageComposer> responses = ArgumentCaptor.forClass(MessageComposer.class);
        verify(client, times(3)).sendResponse(responses.capture());
        assertThickness(responses.getAllValues().get(1), Outgoing.RoomFloorThicknessUpdatedComposer, 1, -2);
        assertThickness(responses.getAllValues().get(2), Outgoing.RoomThicknessComposer, -2, 1);
    }

    @Test
    void floorEditorWithoutRoomSendsNothing() throws Exception {
        GameClient client = clientInRoom(null);
        FloorPlanEditorRequestDoorSettingsEvent event = new FloorPlanEditorRequestDoorSettingsEvent();
        event.client = client;
        event.handle();

        verify(client, never()).sendResponse(any(MessageComposer.class));
    }

    @Test
    void groupMemberUpdatePreservesIdentityRankAndUtf8FieldBoundaries() {
        Guild guild = mock(Guild.class);
        GuildMember member = mock(GuildMember.class);
        when(guild.getId()).thenReturn(101);
        when(member.getRank()).thenReturn(GuildRank.ADMIN);
        when(member.getUserId()).thenReturn(202);
        when(member.getUsername()).thenReturn("Zoë");
        when(member.getLook()).thenReturn("hd-180-1");
        when(member.getJoinDate()).thenReturn(123456789);

        ByteBuf bytes = payload(new GuildMemberUpdateComposer(guild, member), Outgoing.GuildMemberUpdateComposer);
        try {
            assertEquals(101, bytes.readInt());
            assertEquals(1, bytes.readInt());
            assertEquals(202, bytes.readInt());
            assertEquals("Zoë", readString(bytes));
            assertEquals("hd-180-1", readString(bytes));
            assertEquals("123456789", readString(bytes));
            assertFalse(bytes.isReadable());
        } finally {
            bytes.release();
        }

        ByteBuf refresh = payload(new GuildRefreshMembersListComposer(guild), Outgoing.GuildRefreshMembersListComposer);
        try {
            assertEquals(101, refresh.readInt());
            assertEquals(0, refresh.readInt());
            assertFalse(refresh.isReadable());
        } finally {
            refresh.release();
        }
    }

    @Test
    void breedingFailureCarriesExactlyOneReasonForEverySupportedCode() {
        for (int reason = PetBreedingStartFailedComposer.NO_NESTS;
                reason <= PetBreedingStartFailedComposer.TOO_TIRED;
                reason++) {
            ByteBuf bytes =
                    payload(new PetBreedingStartFailedComposer(reason), Outgoing.PetBreedingStartFailedComposer);
            try {
                assertEquals(reason, bytes.readInt());
                assertFalse(bytes.isReadable());
            } finally {
                bytes.release();
            }
        }
    }

    private static GameClient clientInRoom(Room room) {
        GameClient client = mock(GameClient.class);
        Habbo habbo = mock(Habbo.class);
        HabboInfo info = mock(HabboInfo.class);
        when(client.getHabbo()).thenReturn(habbo);
        when(habbo.getHabboInfo()).thenReturn(info);
        when(info.getCurrentRoom()).thenReturn(room);
        return client;
    }

    private static void assertThickness(MessageComposer composer, int header, int first, int second) {
        ByteBuf bytes = payload(composer, header);
        try {
            assertEquals(true, bytes.readBoolean());
            assertEquals(first, bytes.readInt());
            assertEquals(second, bytes.readInt());
            assertFalse(bytes.isReadable());
        } finally {
            bytes.release();
        }
    }

    private static ByteBuf payload(MessageComposer composer, int header) {
        ByteBuf bytes = composer.compose().get();
        assertEquals(bytes.readableBytes() - Integer.BYTES, bytes.readInt());
        assertEquals(header, bytes.readUnsignedShort());
        return bytes;
    }

    private static String readString(ByteBuf bytes) {
        return bytes.readCharSequence(bytes.readUnsignedShort(), StandardCharsets.UTF_8)
                .toString();
    }
}
