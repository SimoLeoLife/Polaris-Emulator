package com.eu.habbo.messages.incoming.mentions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.mentions.MentionManager;
import com.eu.habbo.messages.incoming.MessageHandler;

public class DeleteMentionEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client == null || this.client.getHabbo() == null) return;

        int userId = this.client.getHabbo().getHabboInfo().getId();
        int mentionId = this.packet.readInt();

        if (mentionId <= 0) {
            return;
        }

        MentionManager manager = Emulator.getGameEnvironment().getMentionManager();

        manager.delete(userId, mentionId);
    }
}
