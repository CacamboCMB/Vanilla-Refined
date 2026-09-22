package de.ownmods.inventorysort.client.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("leftPos") int ownmodsSort$left();
    @Accessor("topPos") int ownmodsSort$top();
    @Accessor("imageWidth") int ownmodsSort$width();
}
