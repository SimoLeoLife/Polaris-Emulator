package com.eu.habbo.habbohotel.wired.variablefx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What one player in the room has been sent: the statuses their client is showing, by
 * signature, and the configs it knows. The diff of what they should be seeing against this is
 * what goes out; nothing else does.
 */
public final class WiredVariableFxViewer {
    private final Map<WiredVariableFxStatus.Key, String> sent = new HashMap<>();
    private boolean synced;

    /** A batch to send: the statuses that changed (or are new) and the keys that went away. */
    public record Batch(
            boolean initializeAll, List<WiredVariableFxStatus> updates, List<WiredVariableFxStatus.Key> removed) {
        public boolean isEmpty() {
            return this.updates.isEmpty() && this.removed.isEmpty();
        }
    }

    public boolean isSynced() {
        return this.synced;
    }

    public int sentCount() {
        return this.sent.size();
    }

    /**
     * Works out what differs from what the viewer was last sent and remembers the new state. A
     * status the viewer never had is delivered as "initialize" (drawn without the change
     * animation); the viewer's first batch is a sync rather than a set of changes.
     */
    public Batch diff(Map<WiredVariableFxStatus.Key, WiredVariableFxStatus> wanted) {
        List<WiredVariableFxStatus.Key> removed = new ArrayList<>();
        for (WiredVariableFxStatus.Key key : this.sent.keySet()) {
            if (!wanted.containsKey(key)) removed.add(key);
        }
        for (WiredVariableFxStatus.Key key : removed) {
            this.sent.remove(key);
        }

        List<WiredVariableFxStatus> updates = new ArrayList<>();
        for (Map.Entry<WiredVariableFxStatus.Key, WiredVariableFxStatus> entry : wanted.entrySet()) {
            String signature = entry.getValue().signature();
            String previous = this.sent.get(entry.getKey());

            if (previous != null && previous.equals(signature)) continue;

            this.sent.put(entry.getKey(), signature);
            updates.add(entry.getValue().withInitialize(previous == null));
        }

        boolean initializeAll = !this.synced;
        this.synced = true;

        return new Batch(initializeAll, updates, removed);
    }
}
