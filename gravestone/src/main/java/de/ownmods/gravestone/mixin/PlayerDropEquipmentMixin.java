package de.ownmods.gravestone.mixin;
import de.ownmods.gravestone.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class PlayerDropEquipmentMixin {
    @Inject(method="dropEquipment",at=@At("HEAD"),cancellable=true)
    private void ownmods$grave(ServerLevel level,CallbackInfo ci){
        if((Object)this instanceof ServerPlayer player && GraveEngine.tryCreate(player,level,GravestoneMod.settings()))ci.cancel();
    }
}
