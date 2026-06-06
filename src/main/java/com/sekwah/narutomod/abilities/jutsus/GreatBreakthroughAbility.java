package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Wind Style — Great Breakthrough (combo 231).
 * Releases a powerful gust of wind in a 45° cone, 12 blocks forward.
 * All entities in range: heavy knockback + Slowness I for 3s.
 * Destroys leaves and blows away sand/gravel in the world.
 */
public class GreatBreakthroughAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;
    private static final double RANGE = 12.0;
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(35));
    private static final double KNOCKBACK = 5.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 231;
    }

    @Override
    public int getCooldown() {
        return 7 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double knockback = KNOCKBACK * ninjaData.getRankDamageMultiplier();

        // Entities in cone
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(RANGE * 0.5);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            if (toTarget.dot(look) >= HALF_ANGLE_COS) {
                double dist = eye.distanceTo(target.position());
                if (dist <= RANGE) {
                    target.knockback(knockback, -look.x, -look.z);
                    Vec3 vel = target.getDeltaMovement();
                    target.setDeltaMovement(vel.x, Math.min(vel.y + 0.3, 0.7), vel.z);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 0, false, true));
                }
            }
        }

        // World effects: destroy leaves, blow sand/gravel
        boolean mobGriefing = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(player.level(), player);
        if (mobGriefing) {
            BlockPos playerPos = player.blockPosition();
            int r = (int) RANGE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -2; dy <= 4; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos pos = playerPos.offset(dx, dy, dz);
                        Vec3 toBlock = Vec3.atCenterOf(pos).subtract(eye).normalize();
                        if (toBlock.dot(look) < HALF_ANGLE_COS) continue;
                        double dist = Vec3.atCenterOf(pos).distanceTo(eye);
                        if (dist > RANGE) continue;
                        var state = player.level().getBlockState(pos);
                        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                            player.level().removeBlock(pos, false);
                        } else if (state.getBlock() == Blocks.SAND || state.getBlock() == Blocks.GRAVEL) {
                            player.level().destroyBlock(pos, true);
                        }
                    }
                }
            }
        }

        // Particle cone
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                double t = (i + 1) / 30.0;
                Vec3 pos = eye.add(look.scale(t * RANGE));
                double spread = t * RANGE * 0.35;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        pos.x, pos.y, pos.z, 2, spread * 0.5, spread * 0.25, spread * 0.5, 0.1);
            }
        }
    }
}
