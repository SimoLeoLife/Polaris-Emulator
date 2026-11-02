package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWired;

/** The official AIR addon (1883) save; saved like any effect or extra box. */
public class WiredAddonSaveDataEvent extends WiredEffectSaveDataEvent {
    @Override
    protected InteractionWired.SaveKind saveKind() {
        return InteractionWired.SaveKind.ADDON;
    }
}
