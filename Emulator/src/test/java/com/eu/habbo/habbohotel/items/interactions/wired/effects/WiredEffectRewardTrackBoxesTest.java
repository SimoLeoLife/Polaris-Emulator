package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectProgressAchievementTest.author;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredHotelProgress;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;

/** Habbo's progress and reset reward-track actions: what they accept, store and send back. */
class WiredEffectRewardTrackBoxesTest {

    private static WiredEffectProgressRewardTrack progressBox() {
        return new WiredEffectProgressRewardTrack(11, 1, base(), "", 0, 0);
    }

    private static WiredEffectResetRewardTrack resetBox() {
        return new WiredEffectResetRewardTrack(12, 1, base(), "", 0, 0);
    }

    private static WiredSettings settings(String text, int... params) {
        return new WiredSettings(params, text, new int[0], 0);
    }

    @Test
    void onlyAStaffAuthorMaySaveEitherBox() {
        assertFalse(progressBox().saveData(settings("season_1\tgames", 1, 5, 0), author(false)));
        assertFalse(progressBox().saveData(settings("season_1\tgames", 1, 5, 0), null));
        assertTrue(progressBox().saveData(settings("season_1\tgames", 1, 5, 0), author(true)));

        assertFalse(resetBox().saveData(settings("season_1", 0), author(false)));
        assertFalse(resetBox().saveData(settings("season_1", 0), null));
        assertTrue(resetBox().saveData(settings("season_1", 0), author(true)));
    }

    @Test
    void theProgressBoxNeedsATrackAndATask() {
        WiredEffectProgressRewardTrack box = progressBox();

        assertFalse(box.saveData(settings("season_1", 1, 5, 0), author(true)), "no task");
        assertFalse(box.saveData(settings("season_1\t", 1, 5, 0), author(true)));
        assertFalse(box.saveData(settings("\tgames", 1, 5, 0), author(true)));
        assertFalse(box.saveData(settings("season_1\tgames\textra", 1, 5, 0), author(true)));
        assertFalse(
                box.saveData(settings("season:1\tgames", 1, 5, 0), author(true)), "':' is the allow-list separator");
        assertFalse(box.saveData(settings("x".repeat(65) + "\tgames", 1, 5, 0), author(true)));
    }

    @Test
    void theProgressParamsAreClampedAndSentBackUnderItsCode() {
        WiredEffectProgressRewardTrack box = progressBox();
        assertTrue(box.saveData(settings(" season_1 \t games ", 0, Integer.MAX_VALUE, 999), author(true)));

        assertEquals("season_1", box.getTrackId());
        assertEquals("games", box.getTaskId());
        assertFalse(box.isAddToExisting());
        assertEquals(WiredHotelProgress.MAX_AMOUNT, box.getAmount());
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, box.getUserSource());

        box.saveData(settings("season_1\tgames", 1, -4, WiredSourceUtil.SOURCE_SELECTOR), author(true));
        var body = WiredEffectProgressAchievementTest.body(box);
        assertEquals("season_1\tgames", body.text());
        assertArrayEquals(new int[] {1, 1, WiredSourceUtil.SOURCE_SELECTOR}, body.params());
        assertEquals(WiredEffectType.PROGRESS_REWARD_TRACK.code, body.code());
    }

    @Test
    void theProgressBoxSurvivesTheDatabaseAndTamperedRowsAreCleaned() throws Exception {
        WiredEffectProgressRewardTrack saved = progressBox();
        saved.saveData(settings("season_1\tgames", 0, 30, WiredSourceUtil.SOURCE_SELECTOR), author(true));

        WiredEffectProgressRewardTrack loaded = progressBox();
        loaded.loadWiredData(row(saved.getWiredData()), null);
        assertEquals("season_1", loaded.getTrackId());
        assertEquals("games", loaded.getTaskId());
        assertFalse(loaded.isAddToExisting());
        assertEquals(30, loaded.getAmount());
        assertEquals(WiredSourceUtil.SOURCE_SELECTOR, loaded.getUserSource());

        WiredEffectProgressRewardTrack tampered = progressBox();
        tampered.loadWiredData(
                row(
                        "{\"trackId\":\"a b\",\"taskId\":\"ok\",\"addToExisting\":true,\"amount\":-9,\"delay\":0,\"userSource\":5}"),
                null);
        assertEquals("", tampered.getTrackId());
        assertEquals(1, tampered.getAmount());
        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, tampered.getUserSource());
    }

    @Test
    void theResetBoxTakesOneTrackAndItsUserSource() throws Exception {
        WiredEffectResetRewardTrack box = resetBox();

        assertFalse(box.saveData(settings("", 0), author(true)));
        assertFalse(box.saveData(settings("season 1", 0), author(true)));
        assertTrue(box.saveData(settings("season_1", WiredSourceUtil.SOURCE_SELECTOR), author(true)));

        var body = WiredEffectProgressAchievementTest.body(box);
        assertEquals("season_1", body.text());
        assertArrayEquals(new int[] {WiredSourceUtil.SOURCE_SELECTOR}, body.params());
        assertEquals(WiredEffectType.RESET_REWARD_TRACK.code, body.code());

        WiredEffectResetRewardTrack loaded = resetBox();
        loaded.loadWiredData(row(box.getWiredData()), null);
        assertEquals("season_1", loaded.getTrackId());
        assertEquals(WiredSourceUtil.SOURCE_SELECTOR, loaded.getUserSource());

        loaded.onPickUp();
        assertEquals("", loaded.getTrackId());
    }
}
