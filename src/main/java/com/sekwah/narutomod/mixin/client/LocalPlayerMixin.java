package com.sekwah.narutomod.mixin.client;

import com.mojang.authlib.GameProfile;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.abilities.utility.DoubleJumpAbility;
import com.sekwah.narutomod.capabilities.DoubleJumpData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel p_234112_, GameProfile p_234113_) {
        super(p_234112_, p_234113_);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    public void cancelGravityWallWalk(CallbackInfo ci) {
        this.getCapability(NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) return;
            if (ninjaData.isWallWalkAttached()) {
                // Prevent client-side gravity from fighting wall walk movement
                Vec3 vel = this.getDeltaMovement();
                if (vel.y < 0) {
                    this.setDeltaMovement(vel.x, 0.0, vel.z);
                }
            }
        });
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    public void aiStep(CallbackInfo ci) {
        if (DoubleJumpAbility.airWindow(this)) {
            this.getCapability(NINJA_DATA).ifPresent(ninjaData -> {
                if(!ninjaData.isNinjaModeEnabled() || ninjaData.isWallWalkAttached()) {
                    return;
                }
                DoubleJumpData doubleJumpData = ninjaData.getDoubleJumpData();
                if(doubleJumpData != null) {
                    if(doubleJumpData.canDoubleJumpClient && doubleJumpData.diffUpdateTicksClient > 5) {
                        if(ninjaData.getStamina() < DoubleJumpAbility.STAMINA_COST) {
                            this.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina", Component.translatable("ability.double_jump").withStyle(ChatFormatting.YELLOW)), true);
                            return;
                        }
                        if(ninjaData.getChakra() < DoubleJumpAbility.CHAKRA_COST) {
                            this.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable("ability.double_jump").withStyle(ChatFormatting.YELLOW)), true);
                            return;
                        }
                        doubleJumpData.clientJump();
                        NarutoAbilities.triggerAbility(NarutoAbilities.DOUBLE_JUMP.getId());
                    }
                }
            });
        }
    }

}
