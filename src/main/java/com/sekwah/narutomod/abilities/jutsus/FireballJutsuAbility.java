package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Fire Style: Great Fireball Jutsu — combo 121, CHANNELED.
 *
 * Tap (< 10 ticks): fires a standard fireball instantly.
 * Hold (10–60 ticks): fireball scales in size, damage and explosion radius.
 * Max charge (60 ticks): 3× size, 3× damage, 14-block explosion radius.
 *
 * Uchiha clan bonus: +30% damage at every charge level.
 */
public class FireballJutsuAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    private static final float BASE_CHAKRA = 30f;
    private static final float CHAKRA_PER_TICK = 1.5f;
    private static final int MAX_CHARGE = 60;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 121;
    }

    // Allow instant fire — tap is valid
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
    }

    // Show "charging" message, not "channeling"
    @Override
    public boolean useChargedMessages() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 3 * 20; // base cooldown; no extra for charge to keep it spammable
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (chargeAmount == 0) {
            // Initial activation — pay base cost
            if (ninjaData.getChakra() < BASE_CHAKRA) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
                return false;
            }
            ninjaData.useChakra(BASE_CHAKRA, 30);
        } else {
            // Drain per tick while charging
            if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
                return false; // stop charging, fire immediately
            }
            ninjaData.useChakra(CHAKRA_PER_TICK, 5);
        }
        return true;
    }

    /**
     * While charging: spiral of FLAME + LAVA particles around the player's mouth area.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (player.level() instanceof ServerLevel serverLevel) {
            double speed = 0.8 + (ticksChanneled / 30.0);
            double angle = ticksChanneled * speed;
            double radius = 0.25 + (ticksChanneled / 60.0) * 0.35;

            for (int i = 0; i < 3; i++) {
                double a = angle + (Math.PI * 2.0 / 3.0) * i;
                double px = player.getX() + Math.cos(a) * radius;
                double py = player.getEyeY() - 0.1;
                double pz = player.getZ() + Math.sin(a) * radius;
                serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0.02, 0, 0.01);
                if (ticksChanneled > 20) {
                    serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        final int charge = Math.min(ticksActive, MAX_CHARGE);
        final boolean isUchiha = "uchiha".equals(ninjaData.getClanId());

        ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
            Vec3 shootSpeed = delayedPlayer.getLookAngle();
            FireballJutsuEntity fireball = new FireballJutsuEntity(delayedPlayer, shootSpeed.x, shootSpeed.y, shootSpeed.z);
            fireball.setChargeAmount(charge, isUchiha, ninjaData.getRankDamageMultiplier());
            delayedPlayer.level().addFreshEntity(fireball);
            delayedPlayer.level().playSound(null, delayedPlayer,
                    NarutoSounds.FIREBALL_SHOOT.get(), SoundSource.PLAYERS, 1f + charge / 60f, 1.0f);
        }, charge > 0 ? 5 : 10); // charged fires slightly faster (less wind-up delay)
    }
}
