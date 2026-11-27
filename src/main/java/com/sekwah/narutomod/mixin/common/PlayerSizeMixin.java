package com.sekwah.narutomod.mixin.common;

import com.sekwah.narutomod.util.BaikaScale;
import com.sekwah.narutomod.util.BoulderRoll;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerSizeMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void narutomod$baikaDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> callback) {
        if (pose != Pose.DYING && pose != Pose.SLEEPING) {
            Player player = (Player) (Object) this;
            float roll = BoulderRoll.body(player);
            if (roll > 0) {
                callback.setReturnValue(EntityDimensions.scalable(roll * 2, roll * 2));
                return;
            }
            float scale = BaikaScale.current(player);
            if (scale > 1) {
                EntityDimensions base = callback.getReturnValue();
                callback.setReturnValue(EntityDimensions.scalable(base.width * scale, base.height * scale));
            }
        }
    }
}
