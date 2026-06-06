package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

public class ChidoriDashAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 40.0F;
    private static final double DASH_DISTANCE = 10.0D;
    private static final double HIT_RADIUS = 1.25D;
    private static final DustParticleOptions CHIDORI_PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 2121;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"uchiha".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uchiha",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getSharinganLevel() < 2) {
            player.displayClientMessage(Component.translatable("jutsu.fail.sharingan.two_tomoe",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setChidoriTicks(0);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = start.add(look.scale(DASH_DISTANCE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 destination = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation().subtract(look.scale(0.45D));
        double distance = start.distanceTo(destination);

        List<LivingEntity> targets = findTargets(player, start, look, distance);
        DamageSource source = NarutoDamageTypes.getDamageSource(player.level(), NarutoDamageTypes.CHIDORI, player, player);
        for (LivingEntity target : targets) {
            damageTarget(target, source, ninjaData);
        }

        player.setDeltaMovement(look.x * 2.4D, Math.max(look.y * 0.5D, 0.08D), look.z * 2.4D);
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            spawnDashParticles(serverLevel, start, destination);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.CHIDORI.get();
    }

    private List<LivingEntity> findTargets(Player player, Vec3 start, Vec3 look, double distance) {
        Vec3 path = look.scale(distance);
        AABB searchBox = player.getBoundingBox().expandTowards(path).inflate(HIT_RADIUS);
        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
                    if (entity == player || entity.isSpectator() || !entity.isAlive()) {
                        return false;
                    }
                    Vec3 relative = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D).subtract(start);
                    double projection = relative.dot(look);
                    if (projection < 0.0D || projection > distance) {
                        return false;
                    }
                    Vec3 closest = start.add(look.scale(projection));
                    Vec3 targetCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
                    return targetCenter.distanceTo(closest) <= HIT_RADIUS;
                }).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.position().subtract(start).dot(look)))
                .toList();
    }

    private void damageTarget(LivingEntity target, DamageSource source, INinjaData ninjaData) {
        float damageMultiplier = ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
        if (target instanceof Player targetPlayer) {
            float damage = 16.0F * damageMultiplier;
            if (ninjaData.getNinjaRank() < 4) {
                damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
            }
            if (damage > 0.0F) {
                target.hurt(source, damage);
            }
        } else {
            target.hurt(source, 22.0F * damageMultiplier);
        }
    }

    private void spawnDashParticles(ServerLevel serverLevel, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        int steps = Math.max(12, (int) (diff.length() * 2.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(diff.scale(i / (double) steps));
            serverLevel.sendParticles(CHIDORI_PARTICLE, pos.x, pos.y, pos.z, 1, 0.08D, 0.08D, 0.08D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.08D, 0.08D, 0.08D, 0.04D);
        }
    }
}
