package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Banshō Ten'in — Deva Path (combo 1223).
 * The inverse of Shinra Tensei: drags whatever the user is looking at straight to them.
 * Cheap and low-cooldown, since the payoff is positioning rather than damage — pull a
 * target out of cover and finish them by hand. Loose items come along for the ride.
 */
public class BanshoTeninAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;
    private static final double RANGE = 20.0;
    private static final double PULL_STRENGTH = 1.4;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The pull lasts as long as the outstretched hand does. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 1223;
    }

    @Override
    public String requiredEye() {
        return "rinnegan_path:deva";
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.ENDERMAN_TELEPORT;
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
        Vec3 self = player.position();
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target != null) {
            dragToward(target, self);
        }

        // Anything loose nearby is dragged in too — the pull is not selective
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(RANGE * 0.5))) {
            dragToward(item, self);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle().normalize();
            // Streaks converging on the caster's hand
            for (int step = 10; step >= 1; step--) {
                Vec3 point = player.getEyePosition().add(look.scale(step));
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        point.x, point.y, point.z, 3, 0.1, 0.1, 0.1, 0.01);
            }
            NarutoParticles.spawnRing(serverLevel, self.add(0, 1.0, 0), 1.2, 20, ParticleTypes.PORTAL);
        }
    }

    private static void dragToward(net.minecraft.world.entity.Entity entity, Vec3 destination) {
        Vec3 pull = destination.subtract(entity.position()).normalize().scale(PULL_STRENGTH);
        entity.setDeltaMovement(pull.x, pull.y + 0.25, pull.z);
        entity.hurtMarked = true;
    }
}
