package de.ownmods.settings.client;
import de.ownmods.settings.api.ManagedMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Compatibility redirect for older internal callers; there is only one settings UI. */
final class ModSettingsScreen extends Screen {
    private final Screen parent; private final ManagedMod mod;
    ModSettingsScreen(Screen parent, ManagedMod mod) { super(Component.translatable(mod.titleKey())); this.parent=parent; this.mod=mod; }
    @Override protected void init() { minecraft.gui.setScreen(new IconSettingsScreen(parent,mod)); }
}
