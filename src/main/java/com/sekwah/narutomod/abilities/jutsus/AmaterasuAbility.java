package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class AmaterasuAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 70.0F;
    private static final double RANGE = 32.0D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 113;
    }

    /** Baseline Mangekyo technique — every awakened Mangekyo can light the black flame. */
    @Override
    public String requiredEye() {
        return "sharingan_ms";
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
        Vec3 target = findTarget(player);

        // Ignition burst at the exact cast moment — black flame igniting, before the entity
        // takes over with its own ongoing particle loop.
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.x, target.y, target.z, 30, 0.4, 0.4, 0.4, 0.05);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.6F),
                    target.x, target.y, target.z, 20, 0.3, 0.3, 0.3, 0.02);
        }

        AmaterasuFireEntity fire = new AmaterasuFireEntity(player.level(), player, target.x, target.y, target.z);
        fire.setDamageMultiplier(ninjaData.getRankDamageMultiplier());
        player.level().addFreshEntity(fire);
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WARDEN_SONIC_BOOM;
    }

    private Vec3 findTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        HitResult blockHit = player.pick(RANGE, 0.0F, false);
        Vec3 blockTarget = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        double blockDistance = eye.distanceToSqr(blockTarget);

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, searchBox,
                entity -> canTarget(player, entity));
        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= blockDistance) {
            return entityHit.getEntity().position().add(0.0D, entityHit.getEntity().getBbHeight() * 0.5D, 0.0D);
        }
        return blockTarget;
    }

    private boolean canTarget(Player player, Entity entity) {
        return entity != player && !entity.isSpectator() && entity.isPickable();
    }
}
