package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.util.HotelDateTimeUtil;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The time in a room's wired timezone (the settings tab), or the hotel's when it has none, and when
 * the room's wired timers were last reset, to the millisecond.
 */
public final class WiredRoomTime {
    private static final Map<Integer, Long> TIMER_RESETS_MS = new ConcurrentHashMap<>();
    /** Parsed timezones by the text rooms store; an unknown zone maps to an empty Optional. */
    private static final Map<String, Optional<ZoneId>> ZONES = new ConcurrentHashMap<>();

    private static final int MAX_CACHED_ZONES = 1_024;

    private WiredRoomTime() {}

    public static void forgetRoom(int roomId) {
        TIMER_RESETS_MS.remove(roomId);
    }

    public static void markTimersReset(Room room, long nowMs) {
        if (room != null) {
            TIMER_RESETS_MS.put(room.getId(), nowMs);
        }
    }

    /** Milliseconds since the timers were reset; before any reset, from the room's whole-second stamp. */
    public static long millisSinceTimerReset(Room room, long nowMs) {
        if (room == null) {
            return 0L;
        }

        Long resetMs = TIMER_RESETS_MS.get(room.getId());
        long since = (resetMs != null) ? resetMs : room.getLastTimerReset() * 1000L;
        return Math.max(0L, nowMs - since);
    }

    public static ZonedDateTime now(Room room) {
        return ZonedDateTime.now(zoneFor(room));
    }

    public static ZoneId zoneFor(Room room) {
        String timezone = (room != null) ? room.getWiredTimezone() : null;

        if (timezone == null || timezone.isBlank()) {
            return HotelDateTimeUtil.getZoneId();
        }

        Optional<ZoneId> zone = ZONES.get(timezone);
        if (zone == null) {
            zone = parse(timezone);
            if (ZONES.size() >= MAX_CACHED_ZONES) {
                ZONES.clear();
            }
            ZONES.put(timezone, zone);
        }
        return zone.orElseGet(HotelDateTimeUtil::getZoneId);
    }

    private static Optional<ZoneId> parse(String timezone) {
        try {
            return Optional.of(ZoneId.of(timezone.trim()));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }
}
