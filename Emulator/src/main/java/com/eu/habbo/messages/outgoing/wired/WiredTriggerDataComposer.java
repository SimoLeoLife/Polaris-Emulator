package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.protocol.WiredDefinitionWire;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class WiredTriggerDataComposer extends MessageComposer {
    private final InteractionWiredTrigger trigger;
    private final Room room;

    public WiredTriggerDataComposer(InteractionWiredTrigger trigger, Room room) {
        this.trigger = trigger;
        this.room = room;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredTriggerDataComposer);
        // Each box still writes the legacy layout; it goes out in the official one.
        ServerMessage legacy = new ServerMessage(Outgoing.WiredTriggerDataComposer);
        this.trigger.serializeWiredData(legacy, this.room);
        WiredDefinitionWire.writeOfficial(legacy, WiredDefinitionWire.Holder.TRIGGER, this.response, this.trigger);
        this.trigger.needsUpdate(true);
        return this.response;
    }

    public InteractionWiredTrigger getTrigger() {
        return trigger;
    }

    public Room getRoom() {
        return room;
    }
}
