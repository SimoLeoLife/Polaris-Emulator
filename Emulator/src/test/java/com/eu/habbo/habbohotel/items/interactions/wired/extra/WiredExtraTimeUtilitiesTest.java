package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.messages.ServerMessage;
import io.netty.buffer.ByteBuf;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WiredExtraTimeUtilitiesTest {
    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");
    // 2024-03-10 23:30:15 UTC, a Sunday; 00:30:15 on Monday 11 March in Amsterdam.
    private static final int INSTANT =
            (int) ZonedDateTime.of(2024, 3, 10, 23, 30, 15, 0, ZoneOffset.UTC).toEpochSecond();

    private static WiredExtraTimeUtilities box() {
        Item base = mock(Item.class);
        when(base.getSpriteId()).thenReturn(55);
        return new WiredExtraTimeUtilities(9, 1, base, "", 0, 0);
    }

    private static WiredExtraTimeUtilities box(int mask, int mode) {
        WiredExtraTimeUtilities box = box();
        box.saveData(new WiredSettings(new int[] {mask, mode}, "", new int[0], 0), null);
        return box;
    }

    private static int bits(int... ids) {
        int mask = 0;
        for (int id : ids) mask |= 1 << id;
        return mask;
    }

    private static ResultSet row(String wiredData) throws SQLException {
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data")).thenReturn(wiredData);
        return set;
    }

    @Test
    void saveKeepsOnlyKnownSubVariablesAndModes() {
        WiredExtraTimeUtilities box = box(-1, 7);

        assertEquals(0x7FE | 0x7F00000, box.getSubvariableMask());
        assertEquals(WiredExtraTimeUtilities.MODE_VALUE, box.getMode());
        assertFalse(box.hasSubvariable(0));
        assertFalse(box.hasSubvariable(11));
        assertFalse(box.hasSubvariable(19));
        assertTrue(box.hasSubvariable(WiredExtraTimeUtilities.SUB_YEAR));
        assertTrue(box.hasSubvariable(WiredExtraTimeUtilities.SUB_MONTHS));
        assertEquals(17, box.getSelectedSubvariables().size());

        box.saveData(new WiredSettings(new int[] {bits(4)}, "", new int[0], 0), null);
        assertEquals(List.of(4), box.getSelectedSubvariables());
        assertEquals(WiredExtraTimeUtilities.MODE_VALUE, box.getMode());

        box.saveData(new WiredSettings(new int[0], "3", new int[0], 0), null);
        assertEquals(0, box.getSubvariableMask());
    }

    @Test
    void editorGetsMaskAndModeBack() {
        WiredExtraTimeUtilities box = box(bits(4, 10, 24), WiredExtraTimeUtilities.MODE_LAST_UPDATE_TIME);
        ServerMessage message = new ServerMessage(1);
        box.serializeWiredData(message, null);
        ByteBuf buffer = message.get();
        try {
            buffer.readInt();
            buffer.readShort();
            buffer.readByte();
            buffer.readInt();
            buffer.readInt();
            assertEquals(55, buffer.readInt());
            assertEquals(9, buffer.readInt());
            assertEquals(0, buffer.readShort());
            int[] params = new int[buffer.readInt()];
            for (int i = 0; i < params.length; i++) params[i] = buffer.readInt();
            assertArrayEquals(new int[] {bits(4, 10, 24), WiredExtraTimeUtilities.MODE_LAST_UPDATE_TIME}, params);
            buffer.readInt();
            assertEquals(WiredExtraTimeUtilities.CODE, buffer.readInt());
        } finally {
            buffer.release();
        }
    }

    @Test
    void savedDataRoundTripsAndOldSavesStillLoad() throws Exception {
        WiredExtraTimeUtilities saved = box(bits(1, 26), WiredExtraTimeUtilities.MODE_CREATION_TIME);
        WiredExtraTimeUtilities loaded = box();
        loaded.loadWiredData(row(saved.getWiredData()), null);
        assertEquals(bits(1, 26), loaded.getSubvariableMask());
        assertEquals(WiredExtraTimeUtilities.MODE_CREATION_TIME, loaded.getMode());

        loaded.loadWiredData(row("{\"timeUnit\":3}"), null);
        assertEquals(WiredExtraTimeUtilities.UNIT_HOURS, loaded.getTimeUnit());
        assertEquals(0, loaded.getSubvariableMask());
        assertEquals(WiredExtraTimeUtilities.MODE_VALUE, loaded.getMode());

        loaded.loadWiredData(row("0"), null);
        assertEquals(WiredExtraTimeUtilities.UNIT_MILLISECONDS, loaded.getTimeUnit());
        assertEquals(0, loaded.getSubvariableMask());

        loaded.loadWiredData(row("{\"subvariableMask\":-1,\"mode\":9}"), null);
        assertEquals(0x7FE | 0x7F00000, loaded.getSubvariableMask());
        assertEquals(WiredExtraTimeUtilities.MODE_VALUE, loaded.getMode());
    }

    @Test
    void calendarPartsUseTheGivenZone() {
        WiredExtraTimeUtilities box = box(-1, WiredExtraTimeUtilities.MODE_VALUE);

        assertEquals(0, box.derive(WiredExtraTimeUtilities.SUB_MILLISECOND_OF_SECOND, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(15, box.derive(WiredExtraTimeUtilities.SUB_SECONDS_OF_MINUTE, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(30, box.derive(WiredExtraTimeUtilities.SUB_MINUTE_OF_HOUR, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(0, box.derive(WiredExtraTimeUtilities.SUB_HOUR_OF_DAY, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(1, box.derive(WiredExtraTimeUtilities.SUB_DAY_OF_WEEK, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(11, box.derive(WiredExtraTimeUtilities.SUB_DAY_OF_MONTH, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(71, box.derive(WiredExtraTimeUtilities.SUB_DAY_OF_YEAR, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(11, box.derive(WiredExtraTimeUtilities.SUB_WEEK_OF_YEAR, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(3, box.derive(WiredExtraTimeUtilities.SUB_MONTH_OF_YEAR, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(2024, box.derive(WiredExtraTimeUtilities.SUB_YEAR, INSTANT, 0, 0, AMSTERDAM));

        assertEquals(23, box.derive(WiredExtraTimeUtilities.SUB_HOUR_OF_DAY, INSTANT, 0, 0, ZoneOffset.UTC));
        assertEquals(7, box.derive(WiredExtraTimeUtilities.SUB_DAY_OF_WEEK, INSTANT, 0, 0, ZoneOffset.UTC));
        assertEquals(10, box.derive(WiredExtraTimeUtilities.SUB_WEEK_OF_YEAR, INSTANT, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void unitsCountFromNineteenSeventy() {
        WiredExtraTimeUtilities box = box(-1, WiredExtraTimeUtilities.MODE_VALUE);

        assertEquals(Integer.MAX_VALUE, box.derive(WiredExtraTimeUtilities.SUB_MILLISECONDS, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(90_000, box.derive(WiredExtraTimeUtilities.SUB_MILLISECONDS, 90, 0, 0, AMSTERDAM));
        assertEquals(INSTANT, box.derive(WiredExtraTimeUtilities.SUB_SECONDS, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(INSTANT / 60, box.derive(WiredExtraTimeUtilities.SUB_MINUTES, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(INSTANT / 3600, box.derive(WiredExtraTimeUtilities.SUB_HOURS, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(19_792, box.derive(WiredExtraTimeUtilities.SUB_DAYS, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(2_827, box.derive(WiredExtraTimeUtilities.SUB_WEEKS, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(650, box.derive(WiredExtraTimeUtilities.SUB_MONTHS, INSTANT, 0, 0, AMSTERDAM));
        assertEquals(0, box.derive(WiredExtraTimeUtilities.SUB_DAYS, -5, 0, 0, AMSTERDAM));
    }

    @Test
    void modesPickTheValueOrATimestamp() {
        int created = INSTANT - 86_400 * 3;
        int updated = INSTANT - 60;

        WiredExtraTimeUtilities byValue = box(bits(24), WiredExtraTimeUtilities.MODE_VALUE);
        assertEquals(19_792, byValue.derive(24, INSTANT, created, updated, AMSTERDAM));
        assertNull(byValue.derive(24, null, created, updated, AMSTERDAM));
        assertFalse(byValue.readsTimestamps());

        WiredExtraTimeUtilities byCreation = box(bits(24), WiredExtraTimeUtilities.MODE_CREATION_TIME);
        assertEquals(19_789, byCreation.derive(24, null, created, updated, AMSTERDAM));
        assertNull(byCreation.derive(24, 5, 0, updated, AMSTERDAM));
        assertTrue(byCreation.readsTimestamps());

        WiredExtraTimeUtilities byUpdate = box(bits(22), WiredExtraTimeUtilities.MODE_LAST_UPDATE_TIME);
        assertEquals(updated / 60, byUpdate.derive(22, 0, created, updated, AMSTERDAM));
        assertNull(byUpdate.derive(22, 0, created, 0, AMSTERDAM));
    }

    @Test
    void subVariableNames() {
        WiredExtraTimeUtilities box = box();
        assertEquals("millisecond_of_second", box.subvariableKey(1));
        assertEquals("week_of_year", box.subvariableKey(8));
        assertEquals("year", box.subvariableKey(10));
        assertEquals("millisecond", box.subvariableKey(20));
        assertEquals("month", box.subvariableKey(26));
    }
}
