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
 * Wood Release: Great Forest Burial - a run of roots that tears up out of the ground and
 * walks away from the caster, spearing whatever stands in the line.
 *
 * Erupts in stages rather than all at once, so it reads as something growing toward you
 * and can actually be dodged by moving. The roots are temporary and are taken back down
 * with the rest of the technique.
 */
public class WoodBurialAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 70f;
    private static final float ROOT_DAMAGE = 9f;
    private static final int ROOT_COUNT = 6;
    private static final double ROOT_SPACING = 2.0;
    private static final int ROOT_HEIGHT = 3;
    private static final int LIFESPAN = 160;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3212;
    }

    @Override
    public String requiredClan() {
        return "senju";
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 6;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 6;
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.ROOTED_DIRT_BREAK;
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
        float damage = ROOT_DAMAGE * ninjaData.getRankDamageMultiplier();

        // One tracker for the whole run, so every root comes back down together.
        EarthWallEntity roots = new EarthWallEntity(NarutoEntities.EARTH_WALL.get(), player.level());
        roots.setPos(player.position());
        roots.setLifespan(LIFESPAN);
        player.level().addFreshEntity(roots);

        for (int i = 1; i <= ROOT_COUNT; i++) {
            double distance = i * ROOT_SPACING;
            BlockPos root = groundBelow(player, player.getX() + forward.x * distance,
                    player.getZ() + forward.z * distance);
            final int step = i;
            ninjaData.scheduleDelayedTickEvent(p -> eruptRoot(p, roots, root, damage, step), i * 3);
        }
    }

    private void eruptRoot(Player player, EarthWallEntity roots, BlockPos root, float damage, int step) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<BlockPos> column = new ArrayList<>();
        for (int h = 0; h < ROOT_HEIGHT; h++) {
            column.add(root.above(h));
        }
        roots.placeWall(column, Blocks.OAK_LOG);

        for (LivingEntity caught : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(root).inflate(1.1, ROOT_HEIGHT, 1.1), e -> e != player && e.isAlive())) {
            caught.hurt(player.damageSources().playerAttack(player), damage);
            // Held rather than launched: the roots close around you, they do not punt you.
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, true));
            caught.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128, false, false));
        }

        NarutoParticles.spawnBurst(serverLevel, Vec3.atCenterOf(root.above(1)), 18, 0.5,
                NarutoParticles.LOG_BROWN);
        serverLevel.playSound(null, root, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS,
                1.0f, 0.6f + step * 0.05f);
    }

    /** Finds the surface under a column, so roots erupt from the ground and not mid-air. */
    private BlockPos groundBelow(Player player, double x, double z) {
        BlockPos pos = BlockPos.containing(x, player.getY() + 1, z);
        int min = player.level().getMinBuildHeight();
        while (pos.getY() > min && player.level().getBlockState(pos.below()).isAir()) {
            pos = pos.below();
        }
        return pos;
    }
}
