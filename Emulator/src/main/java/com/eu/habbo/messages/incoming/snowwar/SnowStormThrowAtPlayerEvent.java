package com.eu.habbo.messages.incoming.snowwar;

import com.eu.habbo.habbohotel.games.snowwar.SnowWarGame;
import com.eu.habbo.habbohotel.games.snowwar.SnowWarGamePlayer;
import com.eu.habbo.habbohotel.games.snowwar.SnowWarManager;
import com.eu.habbo.messages.incoming.MessageHandler;

public class SnowStormThrowAtPlayerEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null) {
            return;
        }

        int userId = this.client.getHabbo().getHabboInfo().getId();
        if (!SnowWarManager.getInstance().allowPacket(userId)) {
            return;
        }

        // targetObjectId, trajectory [, turn, subturn]
        if (!SnowWarPacket.hasInts(this.packet.bytesAvailable(), 2)) {
            SnowWarPacket.reject(userId, "throw at player", "unexpected payload length");
            return;
        }
        int targetObjectId = this.packet.readInt();
        int trajectory = this.packet.readInt();
        if (targetObjectId <= 0 || !SnowWarPacket.isTrajectory(trajectory)) {
            SnowWarPacket.reject(
                    userId, "throw at player", "bad target " + targetObjectId + " or trajectory " + trajectory);
            return;
        }
        SnowWarGame game = SnowWarManager.getInstance().getGameByUserId(userId);

        if (game == null) {
            return;
        }

        SnowWarGamePlayer player = game.getPlayer(userId);
        if (player == null) {
            return;
        }

        game.handleThrowAtPlayer(player, targetObjectId, trajectory);
    }
}
