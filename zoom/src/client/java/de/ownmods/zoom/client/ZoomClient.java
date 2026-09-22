package de.ownmods.zoom.client;
import de.ownmods.settings.client.compat.InputCompat;
import com.mojang.blaze3d.platform.InputConstants;
import de.ownmods.settings.api.ManagedMods;
import de.ownmods.zoom.ZoomSettings;
import de.ownmods.zoom.core.ZoomState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ZoomClient implements ClientModInitializer {
    private static ZoomSettings settings;
    private static KeyMapping key;
    private static final ZoomState STATE=new ZoomState();
    @Override public void onInitializeClient() {
        settings=new ZoomSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/zoom.properties"));
        ManagedMods.register(settings);
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ownmods_zoom","keys"));
        key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ownmods_zoom.zoom",InputCompat.keyboardType(InputConstants.Type.class),InputCompat.code("KEY_C"),category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(!settings.enabled() || client.player==null || client.level==null || client.gui.screen()!=null || !client.isWindowActive()) {
                // A camera frame is not guaranteed while disconnected or while a menu is open.
                STATE.reset();
                while(key.consumeClick()) { /* Drop presses from an invalid context. */ }
            }
        });
    }
    /** Camera.calculateFov is called on the game extraction thread, not the render upload thread. */
    public static float adjust(float fov) {
        if(settings==null || key==null)return fov;
        var mc=Minecraft.getInstance();
        boolean valid=mc.player!=null && mc.level!=null && mc.gui.screen()==null && mc.isWindowActive();
        int clicks=0;while(key.consumeClick())clicks++;
        double factor=STATE.update(settings.enabled(),valid,settings.flag("toggle"),key.isDown(),clicks,
                                  settings.flag("strong"),settings.flag("smooth"),System.nanoTime());
        return ZoomState.fov(fov,factor);
    }
}
