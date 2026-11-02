package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraLabel;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;

/**
 * Official furniture labels and regions (AIR 2237): every label this player may see; the client
 * replaces what it had. Each entry is a type (0 = label), the furni id and the text, then the
 * label's colour, visibility and whether it stays open.
 */
public class WiredLabelsComposer extends MessageComposer {
    public static final int TYPE_LABEL = 0;

    private final List<WiredExtraLabel> labels;

    public WiredLabelsComposer(List<WiredExtraLabel> labels) {
        this.labels = (labels != null) ? List.copyOf(labels) : List.of();
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredLabelsComposer);
        this.response.appendInt(this.labels.size());
        for (WiredExtraLabel label : this.labels) {
            this.response.appendInt(TYPE_LABEL);
            this.response.appendInt(label.getId());
            this.response.appendString(label.getText());
            this.response.appendInt(label.getColor());
            this.response.appendInt(label.getVisibility());
            this.response.appendBoolean(label.isAlwaysOpen());
        }
        return this.response;
    }
}
