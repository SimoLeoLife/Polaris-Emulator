package com.eu.habbo.habbohotel.wired.label;

import com.eu.habbo.WiredPlatform;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraLabel;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.wired.WiredLabelsComposer;
import com.eu.habbo.messages.outgoing.wired.WiredLabelsUpdateComposer;
import com.eu.habbo.threading.ThreadPooling;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends the room's furniture labels (wf_label) the way the official client expects them: every
 * label a player may see when they enter the room or their wired rights change, and afterwards only
 * what changed. A label only wired inspectors may see is left out for everybody else, and reaches
 * them as removed when it is hidden from them.
 */
public final class WiredLabelSupport {
    private WiredLabelSupport() {}

    /** Every label in the room, in placement order. */
    public static List<WiredExtraLabel> labels(Room room) {
        List<WiredExtraLabel> labels = new ArrayList<>();
        if (room == null || room.getRoomSpecialTypes() == null) return labels;

        for (InteractionWiredExtra extra : room.getRoomSpecialTypes().getExtras()) {
            if (extra instanceof WiredExtraLabel label) labels.add(label);
        }
        labels.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return labels;
    }

    /** The labels this player may see. */
    public static List<WiredExtraLabel> visibleTo(Room room, Habbo habbo) {
        List<WiredExtraLabel> visible = new ArrayList<>();
        for (WiredExtraLabel label : labels(room)) {
            if (label.isVisibleTo(room, habbo)) visible.add(label);
        }
        return visible;
    }

    /** The full list for one player: on entering the room, or when their wired rights changed. */
    public static void sendAll(Room room, Habbo habbo) {
        if (room == null || habbo == null || habbo.getClient() == null) return;

        habbo.getClient().sendResponse(new WiredLabelsComposer(visibleTo(room, habbo)));
    }

    /** Everyone in the room gets their full list again. */
    public static void republish(Room room) {
        if (room == null) return;

        for (Habbo habbo : room.getHabbos()) sendAll(room, habbo);
    }

    /** A label was placed or changed: those who may see it get it, everyone else loses it. */
    public static void broadcastChanged(Room room, WiredExtraLabel label) {
        if (room == null || label == null) return;

        for (Habbo habbo : room.getHabbos()) {
            if (habbo == null || habbo.getClient() == null) continue;

            habbo.getClient()
                    .sendResponse(
                            label.isVisibleTo(room, habbo)
                                    ? new WiredLabelsUpdateComposer(List.of(label), List.of())
                                    : new WiredLabelsUpdateComposer(List.of(), List.of(label.getId())));
        }
    }

    /** Writes a changed label to the database on the worker pool, as a saved wired box is. */
    public static void save(WiredExtraLabel label) {
        if (label == null) return;

        label.needsUpdate(true);
        ThreadPooling threading = WiredPlatform.threading();
        if (threading != null) threading.run(label);
    }

    /** A label was picked up. */
    public static void broadcastRemoved(Room room, int labelId) {
        if (room == null) return;

        room.sendComposer(new WiredLabelsUpdateComposer(List.of(), List.of(labelId)).compose());
    }
}
