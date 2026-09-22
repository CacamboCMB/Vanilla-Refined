package de.ownmods.inventorysort;

import de.ownmods.inventorysort.core.SortCatalog;
import de.ownmods.inventorysort.core.SortMode;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-thread adapter. Only tag KEYS are cached; actual membership is read each sort. */
final class ItemSortKeys {
    private static final Map<String, TagKey<Item>> TAGS;
    static {
        var tags = new LinkedHashMap<String, TagKey<Item>>();
        SortCatalog.relevantTags().stream().sorted().forEach(id ->
            tags.put(id, TagKey.create(Registries.ITEM, Identifier.parse(id))));
        TAGS = Map.copyOf(tags);
    }
    private final SortMode mode;
    private final Map<String, String> prefixes = new HashMap<>();
    ItemSortKeys(SortMode mode) { this.mode = Objects.requireNonNull(mode); }
    String key(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(mode == SortMode.ID) return id + "\u0000" + stack.getHoverName().getString();
        String prefix = prefixes.computeIfAbsent(id, ignored -> {
            var memberships = new HashSet<String>();
            TAGS.forEach((tagId, tag) -> { if(stack.is(tag)) memberships.add(tagId); });
            return SortCatalog.prefix(new SortCatalog.Facts(id, stack.getItem() instanceof BlockItem, memberships), mode);
        });
        return prefix + id + "\u0000" + stack.getHoverName().getString();
    }
}
