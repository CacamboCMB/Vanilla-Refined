package de.ownmods.mousewheel.client;

import de.ownmods.mousewheel.MouseWheelSettings;
import de.ownmods.mousewheel.client.mixin.ContainerScreenAccess;
import de.ownmods.mousewheel.core.WheelDirection;
import de.ownmods.settings.api.ManagedMods;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Wheel-only Mouse Tweaks behavior with a fixed destination per wheel direction:
 * wheel up -> storage/container side; wheel down -> player inventory.
 * The invert setting swaps those two destinations.
 */
public final class MouseWheelClient implements ClientModInitializer {
    private MouseWheelSettings settings;

    @Override public void onInitializeClient() {
        settings = new MouseWheelSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/mousewheel.properties"));
        ManagedMods.register(settings);

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container) || screen instanceof CreativeModeInventoryScreen) return;
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                if (!settings.enabled() || verticalAmount == 0.0 || client.player == null || client.gameMode == null) return true;
                if (!container.getMenu().getCarried().isEmpty()) return true;
                Slot hovered = ((ContainerScreenAccess) container).ownmodsWheel$hoveredSlot();
                if (hovered == null || !hovered.isActive()) return true;

                WheelDirection.Destination destination = WheelDirection.destination(verticalAmount, settings.flag("invert"));
                int steps = Math.max(1, Math.min(8, (int) Math.ceil(Math.abs(verticalAmount))));
                boolean handled = false;
                for (int i = 0; i < steps; i++) {
                    if (!transferOne(client, container.getMenu(), hovered, destination)) break;
                    handled = true;
                }
                // false suppresses the screen's own wheel handling only when an item was actually moved.
                return !handled;
            });
        });
    }

    private static boolean transferOne(Minecraft client, AbstractContainerMenu menu, Slot hovered,
                                       WheelDirection.Destination destination) {
        var player = client.player;
        if (player == null || client.gameMode == null || !menu.getCarried().isEmpty()) return false;
        Inventory playerInventory = player.getInventory();
        boolean hoveredIsPlayer = hovered.container == playerInventory;
        boolean hoveredIsDestination = WheelDirection.hoveredIsDestination(hoveredIsPlayer, destination);

        Slot source;
        Slot target;
        if (hoveredIsDestination) {
            // Scrolling toward the side that already contains the hovered slot:
            // pull one matching item from the opposite side into this exact stack.
            target = hovered;
            ItemStack wanted = target.getItem();
            if (wanted.isEmpty() || wanted.getCount() >= target.getMaxStackSize(wanted) || !target.mayPlace(wanted)) return false;
            source = findMatchingSource(menu.slots, target, playerInventory, wanted, player);
            if (source == null) return false;
        } else {
            // Scrolling toward the opposite side: move one item from the hovered stack
            // into a matching/empty slot on the requested destination side.
            source = hovered;
            ItemStack stack = source.getItem();
            if (stack.isEmpty() || !source.mayPickup(player)) return false;
            target = findDestination(menu.slots, source, playerInventory, stack);
            if (target == null) return false;
        }

        if (!source.mayPickup(player)) return false;
        ItemStack moving = source.getItem();
        if (moving.isEmpty() || !canAcceptOne(target, moving)) return false;

        // Use Minecraft's normal container input path, so client state and server packets stay in sync.
        int containerId = menu.containerId;
        client.gameMode.handleContainerInput(containerId, source.index, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(containerId, target.index, 1, ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) {
            client.gameMode.handleContainerInput(containerId, source.index, 0, ContainerInput.PICKUP, player);
        }
        return menu.getCarried().isEmpty();
    }

    private static Slot findMatchingSource(List<Slot> slots, Slot target, Inventory playerInventory, ItemStack wanted,
                                           net.minecraft.world.entity.player.Player player) {
        boolean targetIsPlayer = target.container == playerInventory;
        for (Slot candidate : slots) {
            if (candidate == target || (candidate.container == playerInventory) == targetIsPlayer || !candidate.isActive()) continue;
            ItemStack stack = candidate.getItem();
            if (!stack.isEmpty() && candidate.mayPickup(player) && ItemStack.isSameItemSameComponents(stack, wanted)) return candidate;
        }
        return null;
    }

    private static Slot findDestination(List<Slot> slots, Slot source, Inventory playerInventory, ItemStack moving) {
        boolean sourceIsPlayer = source.container == playerInventory;
        Slot empty = null;
        for (Slot candidate : slots) {
            if (candidate == source || (candidate.container == playerInventory) == sourceIsPlayer || !candidate.isActive()) continue;
            ItemStack existing = candidate.getItem();
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, moving) && canAcceptOne(candidate, moving)) return candidate;
            if (empty == null && existing.isEmpty() && canAcceptOne(candidate, moving)) empty = candidate;
        }
        return empty;
    }

    private static boolean canAcceptOne(Slot target, ItemStack moving) {
        if (!target.mayPlace(moving)) return false;
        ItemStack existing = target.getItem();
        if (existing.isEmpty()) return target.getMaxStackSize(moving) > 0;
        return ItemStack.isSameItemSameComponents(existing, moving)
                && existing.getCount() < Math.min(existing.getMaxStackSize(), target.getMaxStackSize(existing));
    }
}
