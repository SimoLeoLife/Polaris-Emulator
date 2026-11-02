package com.eu.habbo.messages.incoming.mentions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.mentions.MentionManager;
import com.eu.habbo.messages.incoming.MessageHandler;

public class MarkMentionsReadEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client == null || this.client.getHabbo() == null) return;

        int userId = this.client.getHabbo().getHabboInfo().getId();
        int mode = this.packet.readInt();
        int mentionId = this.packet.readInt();

        // Only mode 0 (mark-all) and mode 1 (mark-single) are valid. Reject
        // anything else so a crafted packet can't fall into the mark-all branch
        // by accident.
        if (mode != 0 && mode != 1) {
            return;
        }

        if (mode == 1 && mentionId <= 0) {
            return;
        }

        MentionManager manager = Emulator.getGameEnvironment().getMentionManager();

        manager.markRead(userId, mode, mentionId);
    }
}
