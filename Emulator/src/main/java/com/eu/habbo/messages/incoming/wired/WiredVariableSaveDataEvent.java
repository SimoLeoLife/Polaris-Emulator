package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWired;

/** The official AIR variable definition (2310) save; saved like any effect or extra box. */
public class WiredVariableSaveDataEvent extends WiredEffectSaveDataEvent {
    @Override
    protected InteractionWired.SaveKind saveKind() {
        return InteractionWired.SaveKind.VARIABLE;
    }
}
