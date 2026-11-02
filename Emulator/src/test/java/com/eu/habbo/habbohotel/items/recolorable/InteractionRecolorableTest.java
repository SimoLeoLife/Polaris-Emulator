package com.eu.habbo.habbohotel.items.recolorable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.ItemInteraction;
import com.eu.habbo.habbohotel.items.interactions.InteractionRecolorable;
import org.junit.jupiter.api.Test;

/** A recolourable furni keeps its colour next to its state without the rest of the emulator noticing. */
class InteractionRecolorableTest {

    private static InteractionRecolorable item(String extradata) {
        return new InteractionRecolorable(1, 1, null, extradata, 0, 0);
    }

    @Test
    void storedStateAndColourAreSplit() {
        InteractionRecolorable item = item("1|255,0,10");

        assertEquals("1", item.getExtradata());
        assertEquals(0xFF000A, item.getColor());
        assertEquals("1|255,0,10", item.getDatabaseExtraData());
    }

    @Test
    void aPlacementColourStartsInStateZero() {
        InteractionRecolorable item = item("12,34,56");

        assertEquals("0", item.getExtradata());
        assertEquals(0x0C2238, item.getColor());
    }

    @Test
    void changingTheStateKeepsTheColour() {
        InteractionRecolorable item = item("0|1,2,3");

        item.setExtradata("1");

        assertEquals("1", item.getExtradata());
        assertEquals("1|1,2,3", item.getDatabaseExtraData());
    }

    @Test
    void aFurniWithoutColourStoresOnlyItsState() {
        InteractionRecolorable item = item("0");

        assertFalse(item.hasColor());
        assertEquals("0", item.getDatabaseExtraData());

        item.setColor(0x123456);
        assertTrue(item.hasColor());
        assertEquals("0|18,52,86", item.getDatabaseExtraData());
    }

    private static Item recolorableBase() {
        Item base = mock(Item.class);
        when(base.getName()).thenReturn("bc_quartercircle_recolorable");
        when(base.getInteractionType()).thenReturn(new ItemInteraction("recolorable", InteractionRecolorable.class));
        return base;
    }

    @Test
    void aNewFurniOnlyTakesAColourOfItsPalette() {
        Item base = recolorableBase();

        assertEquals("174,240,216", InteractionRecolorable.placementExtradata(base, "174,240,216", "0"));
        assertEquals(
                "255,255,255", InteractionRecolorable.placementExtradata(base, "1,2,3", "0"), "not in the palette");
        assertEquals("255,255,255", InteractionRecolorable.placementExtradata(base, "999,0,0", "0"));
        assertEquals("255,255,255", InteractionRecolorable.placementExtradata(base, "<script>", "0"));
        assertEquals("255,255,255", InteractionRecolorable.placementExtradata(base, null, "0"));
    }

    @Test
    void aStoredStateAndColourAreKeptWhenValid() {
        Item base = recolorableBase();

        assertEquals("2|174,240,216", InteractionRecolorable.placementExtradata(base, "2|174,240,216", "0"));
        assertEquals("255,255,255", InteractionRecolorable.placementExtradata(base, "x|1,2,3", "0"));
    }
}
