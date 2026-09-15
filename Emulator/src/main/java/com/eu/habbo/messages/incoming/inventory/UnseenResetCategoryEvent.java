package com.eu.habbo.messages.incoming.inventory;

import com.eu.habbo.habbohotel.habbicons.HabbiconService;
import com.eu.habbo.messages.incoming.MessageHandler;
import java.util.List;

/**
 * Official {@code UnseenResetCategoryMessageComposer} (3493): the user opened an inventory tab,
 * so everything the tracker still counted as new in that category has now been seen
 * ({@code FurniModel/BadgesModel/BotsModel/PetsModel.resetUnseenItems}). Category
 * {@link HabbiconService#UNSEEN_CATEGORY} belongs to the habbicon collection and is cleared through
 * {@link HabbiconService#clearUnseen} instead.
 */
public class UnseenResetCategoryEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int category = this.packet.readInt();

        if (category <= 0) {
            return;
        }

        if (category == HabbiconService.UNSEEN_CATEGORY) {
            HabbiconService habbicons = this.client.getHabbo().getHabbiconService();

            if (habbicons != null) {
                habbicons.clearUnseen(this.client.getHabbo().getHabboInfo().getId(), List.of());
            }

            return;
        }

        this.client.getHabbo().getInventory().getUnseenItemsComponent().resetCategory(category);
    }
}
