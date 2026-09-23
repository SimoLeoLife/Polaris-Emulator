package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredProjectileFlight;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * The last flight of every furni a projectile add-on launched in one room. A furni keeps its flight
 * after it lands, so a stack can read how far the shot went; it is dropped when the furni leaves the
 * room or the room unloads, and the oldest flights go first past {@link #MAX_FLIGHTS}.
 */
public final class WiredProjectileFlights {
    public static final int MAX_FLIGHTS = 1_024;

    private final LongSupplier clock;
    private final Map<Integer, WiredProjectileFlight> flights = new LinkedHashMap<>();

    public WiredProjectileFlights() {
        this(System::currentTimeMillis);
    }

    WiredProjectileFlights(LongSupplier clock) {
        this.clock = clock;
    }

    public long now() {
        return this.clock.getAsLong();
    }

    public synchronized void begin(int itemId, WiredProjectileFlight flight) {
        if (flight == null) {
            return;
        }

        this.flights.remove(itemId);
        this.flights.put(itemId, flight);

        while (this.flights.size() > MAX_FLIGHTS) {
            Integer eldest = this.flights.keySet().iterator().next();
            this.flights.remove(eldest);
        }
    }

    public synchronized WiredProjectileFlight get(int itemId) {
        return this.flights.get(itemId);
    }

    public boolean holds(HabboItem item, String key) {
        WiredProjectileFlight flight = (item == null) ? null : this.get(item.getId());
        return flight != null && flight.holds(key, this.now());
    }

    public Integer read(HabboItem item, String key) {
        WiredProjectileFlight flight = (item == null) ? null : this.get(item.getId());
        return (flight == null) ? null : flight.read(key, this.now());
    }

    public synchronized int size() {
        return this.flights.size();
    }

    synchronized void forget(int itemId) {
        this.flights.remove(itemId);
    }

    synchronized void clear() {
        this.flights.clear();
    }
}
