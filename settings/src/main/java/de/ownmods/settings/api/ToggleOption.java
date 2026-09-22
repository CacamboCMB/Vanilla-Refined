package de.ownmods.settings.api;

import java.util.Objects;

/** A client-independent setting descriptor. Keys are stable config keys. */
public record ToggleOption(String key, String labelKey, String tooltipKey, boolean defaultValue) {
    public ToggleOption {
        Objects.requireNonNull(key);
        Objects.requireNonNull(labelKey);
        Objects.requireNonNull(tooltipKey);
        if (!key.matches("[a-z0-9_.:-]+")) throw new IllegalArgumentException("Invalid setting key: " + key);
    }
}
