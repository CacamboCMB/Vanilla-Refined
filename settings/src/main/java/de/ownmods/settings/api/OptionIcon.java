package de.ownmods.settings.api;

import java.util.Objects;

/** Item registry ID and translated section heading; interpreted only by the client. */
public record OptionIcon(String itemId, String groupKey) {
    public OptionIcon {
        Objects.requireNonNull(itemId);
        Objects.requireNonNull(groupKey);
        if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid icon item ID: " + itemId);
        if (groupKey.isBlank()) throw new IllegalArgumentException("Missing icon group");
    }
}
