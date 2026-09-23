package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.WiredFurniMoveStyleComposer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Sends the capability-gated move-style hint (header 5110) ahead of wired movement packets.
 * Clients that never announced {@link GameClient#WIRED_FEATURE_MOVE_STYLE} receive nothing and
 * keep the historical linear animation.
 */
public final class WiredMoveStyleHelper {
    public static final int STYLE_LINEAR = 0;
    public static final int STYLE_DROP = 6;
    /** Habbo's jump strength: the intensity is the signed strength, -1000 to 1000. */
    public static final int STYLE_JUMP = 7;

    public static final int DROP_INTENSITY = 100;

    // Hints raised while a wired firing collects its movements, sent as one packet per style when
    // the collection finishes instead of one packet per moved furni.
    private static final ThreadLocal<PendingHints> PENDING = new ThreadLocal<>();
    private static final int MAX_IDS_PER_HINT = 1_000;

    private static final class PendingHints {
        private Room room;
        private final Map<Long, LinkedHashSet<Integer>> idsByStyle = new LinkedHashMap<>();
    }

    private WiredMoveStyleHelper() {}

    static void beginCollection() {
        PENDING.set(new PendingHints());
    }

    static void discardCollection() {
        PENDING.remove();
    }

    static void finishCollection() {
        PendingHints pending = PENDING.get();
        PENDING.remove();

        if (pending == null || pending.room == null) {
            return;
        }

        for (Map.Entry<Long, LinkedHashSet<Integer>> entry : pending.idsByStyle.entrySet()) {
            int style = (int) (entry.getKey() >> 32);
            int intensity = (int) (long) entry.getKey();
            List<Integer> ids = new ArrayList<>(entry.getValue());

            for (int from = 0; from < ids.size(); from += MAX_IDS_PER_HINT) {
                send(pending.room, ids.subList(from, Math.min(ids.size(), from + MAX_IDS_PER_HINT)), style, intensity);
            }
        }
    }

    public static void broadcast(Room room, Collection<Integer> itemIds, int style, int intensity) {
        if (room == null || itemIds == null || itemIds.isEmpty() || style == STYLE_LINEAR) {
            return;
        }

        PendingHints pending = PENDING.get();
        if (pending != null
                && WiredMoveCarryHelper.isCollectingMovements()
                && (pending.room == null || pending.room == room)) {
            pending.room = room;
            long key = ((long) style << 32) | (intensity & 0xFFFFFFFFL);
            pending.idsByStyle
                    .computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                    .addAll(itemIds);
            return;
        }

        send(room, itemIds, style, intensity);
    }

    private static void send(Room room, Collection<Integer> itemIds, int style, int intensity) {
        var message = new WiredFurniMoveStyleComposer(itemIds, style, intensity).compose();
        for (Habbo habbo : room.getHabbos()) {
            if (habbo == null || habbo.getClient() == null) {
                continue;
            }
            if (habbo.getClient()
                    .supportsWiredFeature(
                            GameClient.WIRED_FEATURE_PROTOCOL_VERSION, GameClient.WIRED_FEATURE_MOVE_STYLE)) {
                habbo.getClient().sendResponse(message);
            }
        }
    }
}
