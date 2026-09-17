package com.eu.habbo.messages.outgoing.habbicons;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public final class UserHabbiconStatusChangedComposer extends MessageComposer {
    private final int id;
    private final int state;

    public UserHabbiconStatusChangedComposer(int id, int state) {
        this.id = id;
        this.state = state;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserHabbiconStatusChangedComposer);
        this.response.appendInt(id);
        this.response.appendInt(state);
        return this.response;
    }
}
