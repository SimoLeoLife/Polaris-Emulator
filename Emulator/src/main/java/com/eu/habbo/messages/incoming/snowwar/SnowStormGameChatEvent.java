package com.eu.habbo.messages.incoming.snowwar;

import com.eu.habbo.habbohotel.games.snowwar.SnowWarGame;
import com.eu.habbo.habbohotel.games.snowwar.SnowWarGamePlayer;
import com.eu.habbo.habbohotel.games.snowwar.SnowWarManager;
import com.eu.habbo.messages.incoming.MessageHandler;

public class SnowStormGameChatEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null) {
            return;
        }

        int userId = this.client.getHabbo().getHabboInfo().getId();
        if (!SnowWarManager.getInstance().allowPacket(userId)) {
            return;
        }

        // The message must be the whole payload: its length prefix has to
        // match the bytes that follow (readString would silently clamp it).
        int available = this.packet.bytesAvailable();
        int declaredLength = available >= Short.BYTES
                ? this.packet
                        .getBuffer()
                        .getUnsignedShort(this.packet.getBuffer().readerIndex())
                : -1;
        if (!SnowWarPacket.isWholeString(available, declaredLength, SnowWarPacket.MAX_CHAT_BYTES)) {
            SnowWarPacket.reject(userId, "game chat", "length prefix does not match the payload");
            return;
        }
        String message = this.packet.readString();
        SnowWarGame game = SnowWarManager.getInstance().getGameByUserId(userId);

        if (game == null) {
            return;
        }

        SnowWarGamePlayer player = game.getPlayer(userId);
        if (player == null) {
            return;
        }

        game.handleChat(player, message);
    }
}
