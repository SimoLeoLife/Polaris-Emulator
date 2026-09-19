package com.eu.habbo.messages.incoming.quests.admin;

import com.eu.habbo.habbohotel.quests.RewardTrackAdmin;

/** Creates or updates a prize. */
public class SaveRewardTrackPrizeEvent extends RewardTrackAdminEvent {
    @Override
    public void handle() throws Exception {
        if (!authorize()) {
            return;
        }
        RewardTrackAdmin.PrizeInput input = new RewardTrackAdmin.PrizeInput(
                this.packet.readString().trim(),
                this.packet.readString().trim(),
                this.packet.readInt(),
                this.packet.readInt(),
                this.packet.readString(),
                this.packet.readString(),
                this.packet.readInt(),
                this.packet.readBoolean(),
                this.packet.readInt());
        this.apply(
                RewardTrackAdmin.ENTITY_PRIZE,
                input.trackId(),
                input.id(),
                RewardTrackAdmin.validate(input),
                () -> RewardTrackAdmin.savePrize(input));
    }
}
