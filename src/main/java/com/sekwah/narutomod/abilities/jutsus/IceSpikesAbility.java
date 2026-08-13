package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthWallEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ice Release: Thousand Flying Water Needles of Death - a field of ice spears bursting up
 * out of the ground in front of the caster.
 *
 * Hyoton is not a nature you can awaken; it is what a ninja who has mastered both Water
 * and Wind can do with them at once, so the gate is on both (see Ability.secondaryElement).
 */
public class IceSpikesAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 65f;
    private static final float SPIKE_DAMAGE = 8f;
    private static final int SPIKE_COUNT = 5;
    private static final double SPIKE_SPACING = 2.2;
    private static final int SPIKE_HEIGHT = 3;
    private static final int LIFESPAN = 140;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Matches the earth version it is modelled on. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 2311;
    }

    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public String secondaryElement() {
        return "wind";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 8;
    }

    @Override
    public int getCooldown() {
        return 14 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.GLASS_BREAK;
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
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        float damage = SPIKE_DAMAGE * ninjaData.getRankDamageMultiplier();

        EarthWallEntity ice = new EarthWallEntity(NarutoEntities.EARTH_WALL.get(), player.level());
        ice.setPos(player.position());
        ice.setLifespan(LIFESPAN);
        player.level().addFreshEntity(ice);

        for (int i = 1; i <= SPIKE_COUNT; i++) {
            double distance = i * SPIKE_SPACING;
            BlockPos root = groundBelow(player, player.getX() + forward.x * distance,
                    player.getZ() + forward.z * distance);
            final int step = i;
            ninjaData.scheduleDelayedTickEvent(p -> eruptSpike(p, ice, root, damage, step), i * 2);
        }
    }

    private void eruptSpike(Player player, EarthWallEntity ice, BlockPos root, float damage, int step) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<BlockPos> column = new ArrayList<>();
        for (int h = 0; h < SPIKE_HEIGHT; h++) {
            column.add(root.above(h));
        }
        ice.placeWall(column, Blocks.PACKED_ICE);

        for (LivingEntity caught : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(root).inflate(1.0, SPIKE_HEIGHT, 1.0), e -> e != player && e.isAlive())) {
            caught.hurt(player.damageSources().playerAttack(player), damage);
            // Ice chills rather than launches: the field is meant to hold a line.
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
        }

        NarutoParticles.spawnBurst(serverLevel, Vec3.atCenterOf(root.above(1)), 20, 0.5,
                NarutoParticles.ICE_PALE);
        serverLevel.playSound(null, root, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                0.9f, 1.2f + step * 0.05f);
    }

    private BlockPos groundBelow(Player player, double x, double z) {
        BlockPos pos = BlockPos.containing(x, player.getY() + 1, z);
        int min = player.level().getMinBuildHeight();
        while (pos.getY() > min && player.level().getBlockState(pos.below()).isAir()) {
            pos = pos.below();
        }
        return pos;
    }
}
