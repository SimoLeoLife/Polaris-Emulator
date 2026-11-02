package com.eu.habbo.messages.incoming.snowwar;

import com.eu.habbo.habbohotel.games.snowwar.SnowWarGame;
import com.eu.habbo.habbohotel.games.snowwar.SnowWarGamePlayer;
import com.eu.habbo.habbohotel.games.snowwar.SnowWarManager;
import com.eu.habbo.messages.incoming.MessageHandler;

public class SnowStormWalkEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null) {
            return;
        }

        int userId = this.client.getHabbo().getHabboInfo().getId();
        if (!SnowWarManager.getInstance().allowPacket(userId)) {
            return;
        }

        // worldX, worldY [, turn, subturn] - the server ignores the turn fields.
        if (!SnowWarPacket.hasInts(this.packet.bytesAvailable(), 2)) {
            SnowWarPacket.reject(userId, "walk", "unexpected payload length");
            return;
        }
        int worldX = this.packet.readInt();
        int worldY = this.packet.readInt();
        SnowWarGame game = SnowWarManager.getInstance().getGameByUserId(userId);

        if (game == null) {
            return;
        }

        SnowWarGamePlayer player = game.getPlayer(userId);
        if (player == null) {
            return;
        }
        if (!SnowWarPacket.isInArena(game, worldX, worldY)) {
            SnowWarPacket.reject(userId, "walk", "target outside the arena");
            return;
        }

        game.handleWalk(player, worldX, worldY);
    }
}
