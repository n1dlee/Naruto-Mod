package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class WaterBulletJutsuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 132;
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 15f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(ninjaData.getChakra() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(30, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Water visibly compressing into the shot rather than a splash at the mouth.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.sekwah.narutomod.util.ElementalVfx.waterLance(serverLevel,
                    player.getEyePosition().add(player.getLookAngle().scale(0.4)),
                    player.getLookAngle(), 1.8);
        }

        for (int i = 0; i < 3; i++) {
            ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
                Vec3 shootSpeed = player.getLookAngle();
                WaterBulletJutsuEntity waterBullet = new WaterBulletJutsuEntity(player, shootSpeed.x, shootSpeed.y, shootSpeed.z);
                waterBullet.setDamageMultiplier(ninjaData.getRankDamageMultiplier());
                waterBullet.setYRot(player.getYRot() - 180);
                player.level().addFreshEntity(waterBullet);
                player.level().playSound(null, player, NarutoSounds.WATER_BULLET_SHOOT.get(), SoundSource.PLAYERS, 1f, 1.0f);
            }, 10 + i * 15);
        }

    }

    @Override
    public int getCooldown() {
        return 4 * 20;
    }
}
