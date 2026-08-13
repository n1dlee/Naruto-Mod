package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Aburame Clan — Kikaichu Swarm (combo 321, TOGGLE).
 * The Aburame host colonies of chakra-eating beetles inside their own body. While the
 * swarm is released it gnaws at everything nearby: periodic damage to enemies in radius,
 * and any chakra the beetles eat from enemy ninja flows back to their host — the
 * signature Aburame attrition game.
 */
public class KikaichuSwarmAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final DustParticleOptions KIKAICHU_DARK =
            new DustParticleOptions(new Vector3f(0.12F, 0.12F, 0.15F), 0.7F);
    private static final float CHAKRA_PER_TICK = 1.0f;
    private static final double RADIUS = 5.0;
    private static final float GNAW_DAMAGE = 2.0f;
    private static final float CHAKRA_EATEN = 15f;
    private static final float CHAKRA_RETURNED = 10f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    /** The swarm leaves the sleeves over several beats. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 321;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateAccess(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateAccess(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_TICK, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RADIUS), e -> e != player && e.isAlive())) {
            target.hurt(player.damageSources().magic(), GNAW_DAMAGE * ninjaData.getRankDamageMultiplier());
            // Beetles eat enemy ninja's chakra and carry it home to the host
            target.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(targetData -> {
                float eaten = Math.min(CHAKRA_EATEN, targetData.getChakra());
                if (eaten > 0) {
                    targetData.useChakra(eaten, 20);
                    ninjaData.addChakra(CHAKRA_RETURNED);
                }
            });
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(KIKAICHU_DARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        8, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        // Ambient beetle cloud orbiting the host
        if (player.tickCount % 2 == 0) {
            double angle = player.tickCount * 0.25 + player.getRandom().nextDouble();
            double radius = 0.8 + player.getRandom().nextDouble() * 0.8;
            player.level().addParticle(KIKAICHU_DARK,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.3 + player.getRandom().nextDouble() * 1.6,
                    player.getZ() + Math.sin(angle) * radius,
                    0, 0.01, 0);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BEE_LOOP_AGGRESSIVE;
    }

    private boolean validateAccess(Player player, INinjaData ninjaData) {
        if (!"aburame".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.aburame",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
