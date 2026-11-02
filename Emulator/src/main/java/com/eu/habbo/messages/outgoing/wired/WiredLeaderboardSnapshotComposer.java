package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.wired.leaderboard.WiredLeaderboardConfig;
import com.eu.habbo.habbohotel.wired.leaderboard.WiredLeaderboardDescriptor;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;

/**
 * Official variable leaderboard snapshot (AIR 1494): every leaderboard panel of the room. Sent on
 * the client's hello and again to the room whenever a panel is added, removed or changed; the client
 * drops panels the snapshot no longer lists.
 */
public class WiredLeaderboardSnapshotComposer extends MessageComposer {
    private final List<WiredLeaderboardDescriptor> descriptors;

    public WiredLeaderboardSnapshotComposer(List<WiredLeaderboardDescriptor> descriptors) {
        this.descriptors = (descriptors != null) ? List.copyOf(descriptors) : List.of();
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredLeaderboardSnapshotComposer);
        this.response.appendInt(this.descriptors.size());
        for (WiredLeaderboardDescriptor descriptor : this.descriptors) {
            this.response.appendInt(descriptor.overlayId());
            this.response.appendInt(descriptor.revision());
            this.response.appendString(descriptor.title());
            this.response.appendString(descriptor.description());
            this.response.appendInt(descriptor.tileX());
            this.response.appendInt(descriptor.tileY());
            this.response.appendInt(descriptor.offsetX());
            this.response.appendInt(descriptor.altitude());
            this.response.appendInt(descriptor.sortMode());
            this.response.appendInt(descriptor.columns().size());
            for (WiredLeaderboardConfig.Column column : descriptor.columns()) {
                this.response.appendInt(column.type());
                this.response.appendString(column.label());
                this.response.appendInt(column.width());
            }
            this.response.appendInt(descriptor.choices().size());
            for (WiredLeaderboardDescriptor.Choice choice : descriptor.choices()) {
                this.response.appendString(choice.variableId());
                this.response.appendString(choice.displayName());
            }
            this.response.appendString(descriptor.defaultVariableId());
            this.response.appendInt(descriptor.pageSizeOption());
        }
        return this.response;
    }
}
