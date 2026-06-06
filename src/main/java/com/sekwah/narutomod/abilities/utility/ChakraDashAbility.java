package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ChakraDashAbility extends Ability implements Ability.Toggled {

    private static final int CHAKRA_COOLDOWN = 15;
    private static final float STAMINA_COST = 0.5F;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 2;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        Vec3 movement = player.getDeltaMovement();
        boolean isMoving = movement.x * movement.x + movement.z * movement.z > 0.001;
        float cost = isMoving ? 0.25f : 0.05f;
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (isMoving && ninjaData.getStamina() < STAMINA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_COOLDOWN);
        if (isMoving) {
            ninjaData.useStamina(STAMINA_COST, CHAKRA_COOLDOWN);
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Movement speed is handled centrally in NinjaData through an attribute modifier.
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        Vec3 movement = player.getDeltaMovement();
        if (movement.x * movement.x + movement.z * movement.z > 0.001) {
            player.level().addParticle(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.1, player.getZ(), 0, 0, 0);
        }
    }
}
