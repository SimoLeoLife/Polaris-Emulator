package com.eu.habbo.messages.incoming.inventory;

import com.eu.habbo.habbohotel.habbicons.HabbiconService;
import com.eu.habbo.habbohotel.users.inventory.UnseenItemsComponent;
import com.eu.habbo.messages.incoming.MessageHandler;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Official {@code UnseenResetItemsMessageComposer} (2343): the listed items of one category have
 * been looked at ({@code UnseenItemTracker.resetItems}). Payload is
 * {@code (category, count, itemId...)}. Category {@link HabbiconService#UNSEEN_CATEGORY} belongs to the
 * habbicon collection and is cleared through {@link HabbiconService#clearUnseen} instead.
 */
public class UnseenResetItemsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int category = this.packet.readInt();
        int count = this.packet.readInt();

        if (category <= 0 || count <= 0 || count > UnseenItemsComponent.MAX_RESET_ITEMS) {
            return;
        }

        Set<Integer> itemIds = new LinkedHashSet<>();

        for (int i = 0; i < count; i++) {
            if (this.packet.bytesAvailable() < 4) {
                return;
            }

            itemIds.add(this.packet.readInt());
        }

        if (category == HabbiconService.UNSEEN_CATEGORY) {
            HabbiconService habbicons = this.client.getHabbo().getHabbiconService();

            if (habbicons != null) {
                habbicons.clearUnseen(this.client.getHabbo().getHabboInfo().getId(), new ArrayList<>(itemIds));
            }

            return;
        }

        this.client.getHabbo().getInventory().getUnseenItemsComponent().resetItems(category, itemIds);
    }
}
