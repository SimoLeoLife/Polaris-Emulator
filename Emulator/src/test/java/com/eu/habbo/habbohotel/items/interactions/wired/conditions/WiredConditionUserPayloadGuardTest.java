package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;

class WiredConditionUserPayloadGuardTest {
    @Test
    void actorDirectionBoundsMaskSourceAndQuantifier() {
        WiredConditionActorDir condition = new WiredConditionActorDir(1, 1, null, "", 0, 0);

        assertEquals(255, condition.normalizeDirectionMask(-1));
        assertEquals(0, condition.normalizeDirectionMask(256));
        assertEquals(
                WiredSourceUtil.SOURCE_CLICKED_USER,
                condition.normalizeUserSource(WiredSourceUtil.SOURCE_CLICKED_USER));
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, condition.normalizeUserSource(123_456));
        assertEquals(1, condition.normalizeQuantifier(1));
        assertEquals(0, condition.normalizeQuantifier(2));
    }

    @Test
    void triggererMatchBoundsEntitySourcesQuantifierAndUsername() {
        WiredConditionTriggererMatch condition = new WiredConditionTriggererMatch(1, 1, null, "", 0, 0);

        assertEquals(WiredConditionTriggererMatch.ENTITY_HABBO, condition.normalizeEntityType(999));
        assertEquals(
                WiredConditionTriggererMatch.ENTITY_PET,
                condition.normalizeEntityType(WiredConditionTriggererMatch.ENTITY_PET));
        assertEquals(6, condition.normalizeEntityType(6), "pets and bots together");
        assertEquals(WiredConditionTriggererMatch.AVATAR_MODE_CERTAIN, condition.normalizeAvatarMode(1));
        assertEquals(WiredConditionTriggererMatch.AVATAR_MODE_ANY, condition.normalizeAvatarMode(2));
        assertEquals(
                WiredSourceUtil.SOURCE_SIGNAL, condition.normalizePrimaryUserSource(WiredSourceUtil.SOURCE_SIGNAL));
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, condition.normalizePrimaryUserSource(900));
        assertEquals(
                WiredConditionTriggererMatch.SOURCE_SPECIFIED_USERNAME,
                condition.normalizeCompareUserSource(WiredConditionTriggererMatch.SOURCE_SPECIFIED_USERNAME));
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, condition.normalizeCompareUserSource(-1));
        assertEquals(1, condition.normalizeQuantifier(1));
        assertEquals(0, condition.normalizeQuantifier(5));
        assertEquals("tester", condition.normalizeUsername("  tester  "));
        assertEquals(
                WiredConditionTriggererMatch.MAX_USERNAME_LENGTH,
                condition.normalizeUsername("x".repeat(200)).length());
    }

    @Test
    void effectAndHandItemZeroMeanAny() throws Exception {
        com.eu.habbo.habbohotel.rooms.RoomUnit wearing =
                org.mockito.Mockito.mock(com.eu.habbo.habbohotel.rooms.RoomUnit.class);
        org.mockito.Mockito.when(wearing.getEffectId()).thenReturn(4);
        org.mockito.Mockito.when(wearing.getHandItem()).thenReturn(7);
        com.eu.habbo.habbohotel.rooms.RoomUnit bare =
                org.mockito.Mockito.mock(com.eu.habbo.habbohotel.rooms.RoomUnit.class);

        WiredConditionHabboHasEffect effect = new WiredConditionHabboHasEffect(1, 1, null, "", 0, 0);
        org.junit.jupiter.api.Assertions.assertTrue(effect.matchesEffect(wearing));
        org.junit.jupiter.api.Assertions.assertFalse(effect.matchesEffect(bare));

        WiredConditionHabboHasHandItem hand = new WiredConditionHabboHasHandItem(1, 1, null, "", 0, 0);
        org.junit.jupiter.api.Assertions.assertTrue(hand.matchesHandItem(wearing));
        org.junit.jupiter.api.Assertions.assertFalse(hand.matchesHandItem(bare));
    }
}
