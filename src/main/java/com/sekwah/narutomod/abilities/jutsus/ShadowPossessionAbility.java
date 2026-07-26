package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Nara Clan — Shadow Possession Jutsu (combo 331).
 * Casts a shadow ray forward along the ground (15 blocks).
 * First mob hit is paralyzed for 6 seconds and forced to mirror the player's movement.
 * Mirror effect: every tick the possessed mob receives a velocity matching the player's movement direction.
 */
public class ShadowPossessionAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 50f;
    private static final int RANGE = 15;
    private static final float RAY_WIDTH = 1.8f;
    private static final int POSSESS_TICKS = 6 * 20; // 6 seconds

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 331;
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WARDEN_SONIC_CHARGE;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"nara".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.nara",
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
        Vec3 look = player.getLookAngle();
        // Project onto ground plane (flat shadow)
        Vec3 shadowDir = new Vec3(look.x, 0, look.z).normalize();
        Vec3 origin = player.position();
        double range = RANGE * ninjaData.getClanJutsuRangeMultiplier();

        // Spawn shadow particles along the ray
        if (player.level() instanceof ServerLevel serverLevel) {
            int steps = Math.max(1, (int) Math.round(range * 3));
            for (int i = 0; i <= steps; i++) {
                Vec3 pos = origin.add(shadowDir.scale(i / 3.0));
                serverLevel.sendParticles(NarutoParticles.SHADOW_PURPLE,
                        pos.x, pos.y + 0.05, pos.z,
                        2, 0.15, 0.01, 0.15, 0.0);
            }
        }

        // Find closest mob along the shadow ray
        Vec3 end = origin.add(shadowDir.scale(range));
        AABB searchBox = new AABB(
                Math.min(origin.x, end.x) - RAY_WIDTH, origin.y - 1, Math.min(origin.z, end.z) - RAY_WIDTH,
                Math.max(origin.x, end.x) + RAY_WIDTH, origin.y + 2, Math.max(origin.z, end.z) + RAY_WIDTH);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && !(e instanceof Player));

        LivingEntity target = candidates.stream()
                .filter(e -> {
                    Vec3 toEntity = e.position().subtract(origin);
                    double proj = toEntity.dot(shadowDir);
                    if (proj < 0 || proj > range) return false;
                    Vec3 closest = origin.add(shadowDir.scale(proj));
                    return e.position().distanceTo(closest) <= RAY_WIDTH;
                })
                .min(Comparator.comparingDouble(e -> e.position().distanceTo(origin)))
                .orElse(null);

        if (target == null) return;

        // Apply possession effect: Slowness V (can't move on their own) + Weakness
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, POSSESS_TICKS, 4, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, POSSESS_TICKS, 1, false, false));

        // Store possessed entity UUID so updateDataServer can mirror movement
        ninjaData.setShadowPossessedTarget(target.getUUID(), POSSESS_TICKS);

        // Visual poof on target
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(NarutoParticles.SHADOW_PURPLE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, 0.3, 0.4, 0.3, 0.02);
        }
    }
}
