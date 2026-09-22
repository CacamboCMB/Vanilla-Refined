package de.ownmods.settings.client;

import de.ownmods.settings.SettingsMod;
import java.util.Comparator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

/** Adds the Vanilla Refined manager to the in-game pause menu. */
public final class SettingsClient implements ClientModInitializer {
    private static final int GAP = 4;
    private static final int BOTTOM_MARGIN = 8;

    @Override public void onInitializeClient() {
        ServerSettingsClient.register();
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof PauseScreen pause) || !pause.showsPauseMenu()) return;

            var widgets = Screens.getWidgets(screen);
            // Screen init/resize can rebuild the pause menu. Never keep duplicate buttons.
            widgets.removeIf(VanillaRefinedButton.class::isInstance);

            // The save/disconnect action is the lowest ordinary button in the vanilla pause menu.
            // Using geometry instead of translated text keeps this working for every language and
            // for both singleplayer (save + title) and multiplayer (disconnect).
            var anchor = widgets.stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .max(Comparator.comparingInt(Button::getY))
                    .orElse(null);
            if (anchor == null) {
                SettingsMod.LOGGER.warn("Vanilla Refined: no bottom pause-menu button found");
                return;
            }

            var button = new VanillaRefinedButton(anchor,
                    () -> client.gui.setScreen(new ModsScreen(screen, 0)));
            widgets.add(button);
            button.arrange(screen.height);

            // Other UI code may reposition the vanilla grid after init. Keep this button bound
            // to the save/disconnect row before input and rendering without changing its width.
            ScreenEvents.beforeTick(screen).register(current -> button.arrange(current.height));
            ScreenEvents.beforeExtract(screen).register((current, graphics, mouseX, mouseY, delta) ->
                    button.arrange(current.height));
        });
    }

    private static final class VanillaRefinedButton extends Button.Plain {
        private final Button anchor;

        private VanillaRefinedButton(Button anchor, Runnable open) {
            super(anchor.getX(), anchor.getY() + anchor.getHeight() + GAP,
                    anchor.getWidth(), anchor.getHeight(), Component.translatable("ownmods.button"),
                    b -> open.run(), DEFAULT_NARRATION);
            this.anchor = anchor;
            setTooltip(Tooltip.create(Component.translatable("ownmods.button.tooltip")));
        }

        private void arrange(int screenHeight) {
            int desiredY = anchor.getY() + anchor.getHeight() + GAP;
            int maxY = Math.max(BOTTOM_MARGIN, screenHeight - getHeight() - BOTTOM_MARGIN);

            // Preserve the requested order even on unusually short GUI viewports: move the
            // save/disconnect button and Vanilla Refined up together instead of overlapping.
            if (desiredY > maxY) {
                int shift = desiredY - maxY;
                anchor.setY(Math.max(BOTTOM_MARGIN, anchor.getY() - shift));
                desiredY = anchor.getY() + anchor.getHeight() + GAP;
            }

            if (getX() != anchor.getX()) setX(anchor.getX());
            if (getWidth() != anchor.getWidth()) setWidth(anchor.getWidth());
            if (getHeight() != anchor.getHeight()) setHeight(anchor.getHeight());
            if (getY() != desiredY) setY(desiredY);
        }
    }
}
