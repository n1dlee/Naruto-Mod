package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kirin — Sasuke's signature Mangekyo technique (combo 1121).
 * Calls down natural lightning on the target: enormous single-strike damage plus a
 * smaller splash on everything around the impact. Canon requires storm clouds overhead,
 * so this demands open sky — underground it simply refuses to fire.
 */
public class KirinAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 80f;
    private static final double RANGE = 24.0;
    private static final float DIRECT_DAMAGE = 34f;
    private static final float SPLASH_DAMAGE = 10f;
    private static final double SPLASH_RADIUS = 4.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1121;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ems";
    }

    @Override
    public String requiredEyeForm() {
        return "sasuke";
    }

    @Override
    public int getCooldown() {
        return 45 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.LIGHTNING_BOLT_THUNDER;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        // Kirin rides a thunderhead — it needs sky above the target to come down.
        BlockPos targetPos = target.blockPosition();
        if (!player.level().canSeeSky(targetPos)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.nosky",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        float multiplier = ninjaData.getRankDamageMultiplier();
        DamageSource source = NarutoDamageTypes.getDamageSource(
                player.level(), NarutoDamageTypes.CHIDORI, player, player);

        target.hurt(source, DIRECT_DAMAGE * multiplier);
        target.setSecondsOnFire(3);

        for (LivingEntity splashed : player.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(SPLASH_RADIUS),
                e -> e != player && e != target && e.isAlive())) {
            splashed.hurt(source, SPLASH_DAMAGE * multiplier);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 impact = target.position();
            // The bolt itself: a column of sparks dropping from the cloud base to the target
            for (int y = 0; y < 24; y++) {
                serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN,
                        impact.x, impact.y + y, impact.z, 3, 0.15, 0.1, 0.15, 0.0);
            }
            NarutoParticles.spawnRing(serverLevel, impact.add(0, 0.2, 0), SPLASH_RADIUS, 40,
                    NarutoParticles.CHIDORI_CYAN);
            serverLevel.playSound(null, targetPos, SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 3.0f, 1.0f);
        }
    }
}
