package com.eu.habbo.messages.incoming.habbicons;

import com.eu.habbo.habbohotel.habbicons.HabbiconService;
import com.eu.habbo.messages.incoming.Incoming;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.AlertPurchaseFailedComposer;
import com.eu.habbo.messages.outgoing.catalog.PurchaseOKComposer;
import com.eu.habbo.messages.outgoing.habbicons.HabbiconInfoComposer;
import com.eu.habbo.messages.outgoing.habbicons.HabbiconShopDataComposer;
import com.eu.habbo.messages.outgoing.habbicons.UserHabbiconsComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HabbiconRequestEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HabbiconRequestEvent.class);
    private static final int SERVER_ERROR = 5;

    @Override
    public void handle() {
        int userId = client.getHabbo().getHabboInfo().getId();
        HabbiconService service = client.getHabbo().getHabbiconService();
        HabbiconService.Action action = action(packet.getMessageId());
        int id = packet.getMessageId() == Incoming.GetHabbiconShopDataEvent ? 0 : packet.readInt();
        try {
            if (action != null) {
                service.change(client.getHabbo(), action, id);
                if (purchase(action)) {
                    client.sendResponse(new PurchaseOKComposer());
                }
            } else if (packet.getMessageId() == Incoming.GetHabbiconInfoEvent) {
                client.sendResponse(
                        new HabbiconInfoComposer(service.load(userId).requireItem(id)));
            } else {
                HabbiconService.Snapshot snapshot = service.load(userId);
                client.sendResponse(new UserHabbiconsComposer(snapshot));
                client.sendResponse(new HabbiconShopDataComposer(snapshot));
                if (!snapshot.unseen().isEmpty()) {
                    client.sendResponse(new AddHabboItemComposer(
                            snapshot.unseen().stream()
                                    .mapToInt(Integer::intValue)
                                    .toArray(),
                            AddHabboItemComposer.AddHabboItemCategory.HABBICON));
                }
            }
        } catch (HabbiconService.Rejected exception) {
            fail(action, exception.code());
        } catch (SQLException exception) {
            LOGGER.error("Unable to process Habbicon request for user {}", userId, exception);
            fail(action, SERVER_ERROR);
        }
    }

    private void fail(HabbiconService.Action action, int code) {
        if (purchase(action)) {
            client.sendResponse(new AlertPurchaseFailedComposer(code));
        }
    }

    private static boolean purchase(HabbiconService.Action action) {
        return action == HabbiconService.Action.BUY
                || action == HabbiconService.Action.BUY_COLLECTION
                || action == HabbiconService.Action.CLAIM;
    }

    private static HabbiconService.Action action(int header) {
        return switch (header) {
            case Incoming.BuyHabbiconEvent -> HabbiconService.Action.BUY;
            case Incoming.BuyHabbiconCollectionEvent -> HabbiconService.Action.BUY_COLLECTION;
            case Incoming.ClaimHabbiconEvent -> HabbiconService.Action.CLAIM;
            case Incoming.FavoriteHabbiconEvent -> HabbiconService.Action.FAVORITE;
            case Incoming.UnfavoriteHabbiconEvent -> HabbiconService.Action.UNFAVORITE;
            default -> null;
        };
    }
}
