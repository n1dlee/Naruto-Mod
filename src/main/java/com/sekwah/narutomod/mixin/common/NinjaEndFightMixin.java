package com.sekwah.narutomod.mixin.common;

import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(EndDragonFight.class)
public abstract class NinjaEndFightMixin {
    @Shadow private boolean dragonKilled;
    @Shadow private boolean previouslyKilled;
    @Shadow private UUID dragonUUID;
    @Shadow @Final private ServerBossEvent dragonEvent;
    @Shadow protected abstract void spawnExitPortal(boolean active);
    @Shadow protected abstract void spawnNewGateway();

    @Inject(method = "createNewDragon", at = @At("HEAD"), cancellable = true)
    private void naruto$openExitWithoutDragon(CallbackInfoReturnable<EnderDragon> callback) {
        if (NarutoConfig.allowOtherHostiles) {
            return;
        }
        this.spawnExitPortal(true);
        if (!this.previouslyKilled) {
            this.spawnNewGateway();
        }
        this.dragonKilled = true;
        this.previouslyKilled = true;
        this.dragonUUID = null;
        this.dragonEvent.setVisible(false);
        callback.setReturnValue(null);
    }

    @Inject(method = "tryRespawn", at = @At("HEAD"), cancellable = true)
    private void naruto$disableDragonRespawn(CallbackInfo callback) {
        if (!NarutoConfig.allowOtherHostiles) {
            callback.cancel();
        }
    }
}
