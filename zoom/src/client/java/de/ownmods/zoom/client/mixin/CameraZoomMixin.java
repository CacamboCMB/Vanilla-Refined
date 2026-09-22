package de.ownmods.zoom.client.mixin;
import de.ownmods.zoom.client.ZoomClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public abstract class CameraZoomMixin {
    @Inject(method="calculateFov(F)F",at=@At("RETURN"),cancellable=true)
    private void ownmodsZoom$fov(float partialTicks,CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ZoomClient.adjust(cir.getReturnValueF()));
    }
}
