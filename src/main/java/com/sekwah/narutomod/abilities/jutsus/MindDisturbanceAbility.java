package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.List;

/**
 * Yamanaka Clan — Mind Disturbance Technique / Shinranshin no Jutsu (combo 322, INSTANT).
 * The Yamanaka reach into enemy minds and yank the strings: every mob caught in the
 * radius is turned against its neighbours (they attack each other), while enemy ninja
 * (players) suffer disorientation instead — full mind-control of another player isn't
 * something Minecraft can express, so canon's "confuse the enemy ranks" reading wins.
 */
public class MindDisturbanceAbility extends Ability implements Ability.Cooldown {

    private static final DustParticleOptions MIND_VIOLET =
            new DustParticleOptions(new Vector3f(0.75F, 0.4F, 0.95F), 1.1F);
    private static final float CHAKRA_COST = 55f;
    private static final double RADIUS = 8.0;
    private static final int CONFUSION_TICKS = 5 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 322;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.EVOKER_PREPARE_ATTACK;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"yamanaka".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.yamanaka",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
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
        double radius = RADIUS * ninjaData.getClanJutsuRangeMultiplier();
        List<LivingEntity> caught = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive());

        List<Mob> mobs = caught.stream()
                .filter(e -> e instanceof Mob)
                .map(e -> (Mob) e)
                .toList();

        // Turn the mobs on each other — each one targets the next in the circle
        if (mobs.size() >= 2) {
            for (int i = 0; i < mobs.size(); i++) {
                Mob puppet = mobs.get(i);
                Mob victim = mobs.get((i + 1) % mobs.size());
                puppet.setTarget(victim);
                puppet.setLastHurtByMob(victim);
            }
        }

        for (LivingEntity target : caught) {
            if (target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CONFUSION_TICKS, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONFUSION_TICKS, 1, false, true));
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                NarutoParticles.spawnRing(serverLevel,
                        target.position().add(0, target.getBbHeight() + 0.3, 0), 0.4, 8, MIND_VIOLET);
            }
        }

        if (mobs.size() < 2 && caught.stream().noneMatch(e -> e instanceof Player)) {
            player.displayClientMessage(Component.literal("No minds in range to disturb!")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }
}
