package de.ownmods.shulkerpreview.client.mixin;
import de.ownmods.shulkerpreview.client.ShulkerPreviewClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerTooltipMixin {
    @Shadow protected Slot hoveredSlot;
    @Inject(method="extractTooltip",at=@At("HEAD"),cancellable=true)
    private void ownmodsPreview$tooltip(GuiGraphicsExtractor g,int mouseX,int mouseY,CallbackInfo ci) {
        var screen=(AbstractContainerScreen<?>)(Object)this;
        if(hoveredSlot!=null && screen.getMenu().getCarried().isEmpty()
            && ShulkerPreviewClient.draw(screen,g,hoveredSlot.getItem(),mouseX,mouseY))ci.cancel();
    }
}
