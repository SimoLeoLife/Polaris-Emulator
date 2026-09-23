package com.eu.habbo.messages.incoming.wired;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eu.habbo.habbohotel.wired.core.WiredRoomDiagnostics;
import com.eu.habbo.messages.outgoing.wired.WiredRoomLogPageComposer;
import java.util.List;
import org.junit.jupiter.api.Test;

class WiredRoomLogsPageEventTest {

    @Test
    void theRoomLogListsTheNewestLineFirst() {
        WiredRoomDiagnostics diagnostics = new WiredRoomDiagnostics(1_000, 100, 1, 50, 150, 70, 5);
        diagnostics.recordWiredLog(1_000L, WiredRoomDiagnostics.Severity.INFO, "first", "wf_act_log", 9);
        diagnostics.recordWiredLog(1_001L, WiredRoomDiagnostics.Severity.INFO, "second", "wf_act_log", 9);
        diagnostics.recordWiredLog(1_002L, WiredRoomDiagnostics.Severity.INFO, "third", "wf_act_log", 9);

        List<WiredRoomLogPageComposer.Entry> entries =
                WiredRoomLogsPageEvent.collect(diagnostics.snapshot(0, 10, 0L, 1_003L), -1, -1, "");

        assertEquals(
                List.of("third", "second", "first"),
                entries.stream()
                        .map(WiredRoomLogPageComposer.Entry::getLogMessage)
                        .toList());
        assertEquals(1L, entries.get(0).getId());
    }
}
