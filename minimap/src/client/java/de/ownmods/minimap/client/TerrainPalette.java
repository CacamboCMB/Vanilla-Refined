package de.ownmods.minimap.client;
import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.world.level.block.state.BlockState;
final class TerrainPalette {private TerrainPalette(){}
    static int color(BlockState state,int y){String p=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();int c;
        if(p.contains("water")||p.contains("seagrass")||p.contains("kelp"))c=0x3F76E4;else if(p.contains("lava"))c=0xD85A28;else if(p.contains("snow")||p.contains("ice"))c=0xEAF4FF;else if(p.contains("sand"))c=0xE2D49A;else if(p.contains("grass")||p.contains("moss"))c=0x72A94D;else if(p.contains("leaves")||p.contains("vine"))c=0x4F8A3D;else if(p.contains("log")||p.contains("wood")||p.contains("planks"))c=0x9A764C;else if(p.contains("netherrack")||p.contains("nether_wart")||p.contains("crimson"))c=0x7C3434;else if(p.contains("warped"))c=0x318C87;else if(p.contains("basalt")||p.contains("blackstone"))c=0x3D3A3E;else if(p.contains("end_stone"))c=0xD8D68E;else if(p.contains("purpur"))c=0xA477A4;else if(p.contains("terracotta"))c=0xB86F55;else if(p.contains("deepslate"))c=0x4A4A50;else if(p.contains("stone")||p.contains("ore")||p.contains("brick"))c=0x818181;else if(p.contains("dirt")||p.contains("mud")||p.contains("podzol"))c=0x79553A;else c=0x8D8A76;
        int shade=Math.max(-24,Math.min(24,(y-64)/5));return shade(c,shade);}
    static int shade(int c,int delta){int r=Math.max(0,Math.min(255,((c>>16)&255)+delta)),g=Math.max(0,Math.min(255,((c>>8)&255)+delta)),b=Math.max(0,Math.min(255,(c&255)+delta));return 0xFF000000|(r<<16)|(g<<8)|b;}
}
