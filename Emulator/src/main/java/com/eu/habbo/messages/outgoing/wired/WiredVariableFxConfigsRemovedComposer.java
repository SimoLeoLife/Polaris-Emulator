package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.Collections;
import java.util.List;

/** Fx boxes that are gone: the client drops the configs and everything drawn by them. */
public class WiredVariableFxConfigsRemovedComposer extends MessageComposer {
    private final List<Integer> configIds;

    public WiredVariableFxConfigsRemovedComposer(List<Integer> configIds) {
        this.configIds = (configIds != null) ? configIds : Collections.emptyList();
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredVariableFxConfigsRemovedComposer);
        this.response.appendInt(this.configIds.size());
        for (int configId : this.configIds) {
            this.response.appendInt(configId);
        }
        return this.response;
    }
}
