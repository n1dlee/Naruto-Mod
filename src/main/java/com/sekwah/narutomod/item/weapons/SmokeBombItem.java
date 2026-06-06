package com.sekwah.narutomod.item.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Smoke Bomb — throwable item that creates a cloud of blindness.
 * On use: instantly creates a smoke cloud at impact point (8 blocks forward or where it hits).
 * All entities within 4 blocks get Blindness II for 3 seconds.
 * Simplified as instant-use (not a projectile entity) for now.
 */
public class SmokeBombItem extends Item {

    private static final double RANGE = 8.0;
    private static final double CLOUD_RADIUS = 4.0;
    private static final int BLINDNESS_DURATION = 3 * 20; // 3 seconds
    private static final int BLINDNESS_AMPLIFIER = 1; // Blindness II

    public SmokeBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // Deploy smoke at 8 blocks forward (or closer if blocked)
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 targetPos = eye.add(look.scale(RANGE));

        // Check for block collision via simple raycast
        var hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                eye, targetPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        final Vec3 smokeCenter = hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                ? hitResult.getLocation() : targetPos;

        // Apply blindness to all living entities in radius (including thrower!)
        AABB area = new AABB(smokeCenter.subtract(CLOUD_RADIUS, CLOUD_RADIUS, CLOUD_RADIUS),
                smokeCenter.add(CLOUD_RADIUS, CLOUD_RADIUS, CLOUD_RADIUS));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e.distanceToSqr(smokeCenter) <= CLOUD_RADIUS * CLOUD_RADIUS);

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLINDNESS_DURATION, 0, false, true));
        }

        // Smoke particles
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 60; i++) {
                double ox = (level.random.nextDouble() - 0.5) * CLOUD_RADIUS * 2;
                double oy = (level.random.nextDouble() - 0.5) * CLOUD_RADIUS;
                double oz = (level.random.nextDouble() - 0.5) * CLOUD_RADIUS * 2;
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        smokeCenter.x + ox, smokeCenter.y + oy + 1.0, smokeCenter.z + oz,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    smokeCenter.x, smokeCenter.y + 1.0, smokeCenter.z,
                    3, 0.5, 0.5, 0.5, 0.0);
        }

        level.playSound(null, smokeCenter.x, smokeCenter.y, smokeCenter.z,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 40); // 2 second cooldown

        return InteractionResultHolder.consume(stack);
    }
}
