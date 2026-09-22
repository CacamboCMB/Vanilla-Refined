package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.NotesSettings;
import de.ownmods.notes.core.*;
import de.ownmods.settings.api.ManagedMods;
import java.io.IOException;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

public final class NotesClient implements ClientModInitializer {
    static NotesSettings settings;
    @Override public void onInitializeClient() {
        settings=new NotesSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/notes.properties"));
        ManagedMods.register(settings);
    }
    static String identity() throws IOException {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null)throw new IOException(UiText.tr("vr.text.02b5f3986f40"));
        var server=mc.getSingleplayerServer();
        if(server!=null)return "singleplayer:"+server.getWorldPath(LevelResource.ROOT).toRealPath();
        var data=mc.getCurrentServer();
        if(data!=null)return "server:"+data.ip.toLowerCase(Locale.ROOT).strip();
        throw new IOException(UiText.tr("vr.text.4c8aa4012870"));
    }
    static final class Session {
        final String identity; final NotesStore store;
        Session() throws IOException {
            identity=identity(); String hash=NotesStore.scopeKey(identity);
            store=new NotesStore(FabricLoader.getInstance().getConfigDir().resolve("ownmods/notes/worlds/"+hash+".properties"),hash);
        }
        void verify() throws IOException {if(!identity.equals(identity()))throw new IOException(UiText.tr("vr.text.5e8bf88c4755"));}
        void save(Note n) throws IOException {verify();store.save(n);}
        void delete(String id) throws IOException {verify();store.delete(id);}
    }
    static Note.Position here() {
        var mc=Minecraft.getInstance(); if(mc.level==null||mc.player==null)return null;
        var p=mc.player.blockPosition();return new Note.Position(mc.level.dimension().identifier().toString(),p.getX(),p.getY(),p.getZ());
    }
    static String biomeHere() {
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return "";
        return mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k->k.identifier().toString()).orElse("");
    }
    static List<NotesCatalog.Entry> biomes() {
        return biomes(NotesCatalog.OVERWORLD);
    }
    static List<NotesCatalog.Entry> biomes(String dimension) {
        var mc=Minecraft.getInstance();var values=new ArrayList<NotesCatalog.Entry>();
        values.add(new NotesCatalog.Entry("",UiText.tr("vr.text.83fe3439a399"),"minecraft:map"));
        if(mc.level!=null)mc.level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().forEach(holder->{
            String id=holder.key().identifier().toString();
            if(!BiomeVisuals.dimensionFor(id).equals(dimension))return;
            String name=Component.translatable("biome."+id.replace(':','.').replace('/','.')).getString();
            values.add(new NotesCatalog.Entry(id,name,BiomeVisuals.iconFor(id)));
        });
        values.sort(Comparator.comparing(NotesCatalog.Entry::label));return values;
    }
    static List<NotesCatalog.Entry> mobs() {
        var values=new ArrayList<NotesCatalog.Entry>();values.add(new NotesCatalog.Entry("",UiText.tr("vr.text.83fe3439a399"),"minecraft:spawner"));
        // Goal mob picker may use all living spawn-egg-backed mobs. Location spawners use spawnerMobs() below.
        for(var id:BuiltInRegistries.ITEM.keySet())if(id.getPath().endsWith("_spawn_egg")) {
            String mob=id.toString().replaceFirst("_spawn_egg$","");
            values.add(new NotesCatalog.Entry(mob,Component.translatable("entity."+mob.replace(':','.')).getString(),id.toString()));
        }
        values.sort(Comparator.comparing(NotesCatalog.Entry::label));return values;
    }
    static List<NotesCatalog.Entry> spawnerMobs() {
        var out=new ArrayList<NotesCatalog.Entry>();
        out.add(new NotesCatalog.Entry("",UiText.tr("vr.text.83fe3439a399"),"minecraft:spawner"));
        out.addAll(NotesLabels.spawnerMobs());
        return out;
    }
    static List<NotesCatalog.Entry> items() {
        var out=new ArrayList<NotesCatalog.Entry>();
        for(var item:BuiltInRegistries.ITEM) {
            String id=BuiltInRegistries.ITEM.getKey(item).toString();if(id.equals("minecraft:air"))continue;
            var stack=new net.minecraft.world.item.ItemStack(item);
            out.add(new NotesCatalog.Entry(id,stack.getHoverName().getString(),id));
        }
        out.sort(Comparator.comparing(NotesCatalog.Entry::label).thenComparing(NotesCatalog.Entry::id));return out;
    }
    /** Current main inventory + hotbar, not cumulative collection and not arbitrary world chests. */
    static Map<String,Integer> inventory() {
        var result=new HashMap<String,Integer>();var mc=Minecraft.getInstance();if(mc.player==null)return result;
        var inv=mc.player.getInventory();
        for(int i=0;i<Math.min(36,inv.getContainerSize());i++) {
            var stack=inv.getItem(i);if(!stack.isEmpty())result.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);
        }
        return result;
    }
}
