package de.ownmods.settings.policy;

/** Thread-confined replay and rate guard. Times are injected, so boundary cases are testable. */
public final class RequestGate {
    private final long interval;
    private long lastId = -1;
    private long next;
    private boolean used;
    public RequestGate(long intervalNanos) {
        if (intervalNanos < 0) throw new IllegalArgumentException("Negative interval"); interval = intervalNanos;
    }
    public boolean allow(long id, long now) {
        if (id < 0 || id <= lastId || (used && now - next < 0)) return false;
        lastId = id; next = now + interval; used = true; return true;
    }
}
