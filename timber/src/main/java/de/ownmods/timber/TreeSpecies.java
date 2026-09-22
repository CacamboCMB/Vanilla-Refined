package de.ownmods.timber;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Natural overworld logs only. Stripped wood and Nether fungi intentionally do not trigger. */
record TreeSpecies(String name, Block log, Block leaves, Block sapling) {
    static final List<TreeSpecies> ALL = List.of(
            new TreeSpecies("oak", Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING),
            new TreeSpecies("spruce", Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING),
            new TreeSpecies("birch", Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING),
            new TreeSpecies("jungle", Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING),
            new TreeSpecies("acacia", Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING),
            new TreeSpecies("dark_oak", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING),
            new TreeSpecies("mangrove", Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, Blocks.MANGROVE_PROPAGULE),
            new TreeSpecies("cherry", Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES, Blocks.CHERRY_SAPLING),
            new TreeSpecies("pale_oak", Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_LEAVES, Blocks.PALE_OAK_SAPLING)
    );
    static TreeSpecies forLog(Block block) {
        for (var species : ALL) if (species.log == block) return species;
        return null;
    }
    static String leafFamily(Block block) {
        for (var species : ALL) if (species.leaves == block) return species.name;
        return "";
    }
}
