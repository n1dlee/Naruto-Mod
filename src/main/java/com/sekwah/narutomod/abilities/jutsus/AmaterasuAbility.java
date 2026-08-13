package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.AmaterasuFlames;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

    /** A held stare - the flame follows the eye, so the eye has to linger. */
    @Override
    public int castPoseTicks() {
        return 20;
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

    /**
     * Sighting the flame onto whatever the caster is looking at.
     *
     * Two outcomes, exactly as in the 1.12.2 mod: a living target catches the black flame
     * personally and carries it with them, while a miss lights the ground and the fire
     * spreads outward from there. Sneaking calls the flames back in instead of casting -
     * Amaterasu cannot be doused, so the wielder being the only one who can end it is the
     * point, not a convenience.
     */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.isShiftKeyDown()) {
            extinguish(player);
            return;
        }
        int power = Math.max(0, ninjaData.getNinjaRank() - 1);

        LivingEntity struck = findLivingTarget(player);
        if (struck != null) {
            AmaterasuFlames.ignite(struck, AmaterasuFlames.DEFAULT_DURATION, power);
            spawnIgnitionBurst(player, struck.position().add(0, struck.getBbHeight() * 0.5, 0));
            return;
        }

        Vec3 target = findTarget(player);
        spawnIgnitionBurst(player, target);
        seedFlame(player, BlockPos.containing(target));
    }

    /** Lights the first free spot at the aim point, and lets the block do the spreading. */
    private void seedFlame(Player player, BlockPos pos) {
        Level level = player.level();
        for (BlockPos candidate : new BlockPos[]{pos, pos.above(), pos.below()}) {
            if (level.getBlockState(candidate).isAir()) {
                level.setBlockAndUpdate(candidate,
                        NarutoBlocks.AMATERASU_FLAME.get().defaultBlockState());
                return;
            }
        }
    }

    /**
     * Calls the black flame back. Clears anything burning within range and puts out the
     * flame blocks around the aim point, which is the only way to stop a burn short.
     */
    private void extinguish(Player player) {
        Level level = player.level();
        for (LivingEntity burning : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), AmaterasuFlames::isBurning)) {
            AmaterasuFlames.extinguish(burning);
        }

        BlockPos centre = BlockPos.containing(findTarget(player));
        int r = 8;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (level.getBlockState(pos).is(NarutoBlocks.AMATERASU_FLAME.get())) {
                level.removeBlock(pos, false);
            }
        }
        player.displayClientMessage(
                Component.translatable("jutsu.amaterasu.extinguished").withStyle(ChatFormatting.GRAY), true);
    }

    private void spawnIgnitionBurst(Player player, Vec3 at) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.05);
            serverLevel.sendParticles(NarutoParticles.AMATERASU_BLACK,
                    at.x, at.y, at.z, 25, 0.35, 0.35, 0.35, 0.02);
        }
    }

    /** The entity in the crosshair, or null if the ray hit terrain first. */
    private LivingEntity findLivingTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        HitResult blockHit = player.pick(RANGE, 0.0F, false);
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : eye.distanceToSqr(blockHit.getLocation());

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, searchBox,
                entity -> canTarget(player, entity));
        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= blockDistance
                && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
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
