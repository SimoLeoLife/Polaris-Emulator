package com.eu.habbo.habbohotel.wired.arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eu.habbo.habbohotel.items.interactions.wired.WiredLargePayload;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraArrayCaptureVariable;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import org.junit.jupiter.api.Test;

class WiredArrayProtocolContractTest {

    @Test
    void keepsBackendLayoutCodesAndBoundedPayloadInSync() {
        assertEquals(126, WiredEffectType.MODIFY_ARRAY.code);
        assertEquals(1014, WiredConditionType.CHECK_ARRAY.code); // not an official condition: 1000 and up
        assertEquals(127, WiredExtraArrayCaptureVariable.CODE);
        assertEquals(32_768, WiredLargePayload.MAX_STRING_PARAM_LENGTH);
    }
}
