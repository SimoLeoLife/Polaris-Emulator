package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.wired.variablefx.WiredVariableFxStatus;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.Collections;
import java.util.List;

/** Statuses a player should stop drawing: a value, a holder or their right to see it went away. */
public class WiredVariableFxStatusRemovedComposer extends MessageComposer {
    private final List<WiredVariableFxStatus.Key> keys;

    public WiredVariableFxStatusRemovedComposer(List<WiredVariableFxStatus.Key> keys) {
        this.keys = (keys != null) ? keys : Collections.emptyList();
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredVariableFxStatusRemovedComposer);
        this.response.appendInt(this.keys.size());
        for (WiredVariableFxStatus.Key key : this.keys) {
            this.response.appendString(key.wireKey());
        }
        return this.response;
    }
}
