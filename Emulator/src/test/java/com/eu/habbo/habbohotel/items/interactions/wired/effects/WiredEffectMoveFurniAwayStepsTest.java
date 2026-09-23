package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Fleeing furni try the step along the longer axis first, then the other, instead of one only. */
class WiredEffectMoveFurniAwayStepsTest {

    @Test
    void theLongerAxisComesFirstAndTheOtherIsTheFallback() {
        // Someone three tiles west and one north: step east first, then south.
        List<int[]> steps = WiredEffectMoveFurniAway.stepsAway(5, 5, 2, 4);

        assertEquals(2, steps.size());
        assertArrayEquals(new int[] {1, 0}, steps.get(0));
        assertArrayEquals(new int[] {0, 1}, steps.get(1));
    }

    @Test
    void inLineThereIsOneWayOutAndOnTheSameTileNone() {
        List<int[]> steps = WiredEffectMoveFurniAway.stepsAway(5, 5, 5, 8);

        assertEquals(1, steps.size());
        assertArrayEquals(new int[] {0, -1}, steps.get(0));
        assertTrue(WiredEffectMoveFurniAway.stepsAway(5, 5, 5, 5).isEmpty());
    }
}
