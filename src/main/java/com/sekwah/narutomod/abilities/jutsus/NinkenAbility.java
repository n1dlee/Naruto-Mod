package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;

/**
 * Inuzuka Clan — Ninken Partner (combo 323, INSTANT).
 * Every Inuzuka fights alongside their ninja dog. Summons a battle-trained ninken —
 * a tamed wolf with warhound stats (30 HP, hard bite) that fights for the summoner —
 * which returns home (despawns) after two minutes.
 */
public class NinkenAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 40f;
    private static final int DURATION_TICKS = 2 * 60 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A summoning, just a smaller one. */
    @Override
    public int castPoseTicks() {
        return 16;
    }

    @Override
    public long defaultCombo() {
        return 323;
    }

    @Override
    public int getCooldown() {
        return 60 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WOLF_HOWL;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"inuzuka".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.inuzuka",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Wolf ninken = EntityType.WOLF.create(serverLevel);
        if (ninken == null) {
            return;
        }
        ninken.setPos(player.getX() + 1.0, player.getY(), player.getZ());
        ninken.tame(player);
        ninken.setCustomName(Component.literal(player.getName().getString() + "'s Ninken"));

        // Warhound stats — this is a battle partner, not a pet
        var health = ninken.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(30.0D);
        ninken.setHealth(30.0F);
        var attack = ninken.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(8.0D * ninjaData.getRankDamageMultiplier());
        var speed = ninken.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.38D);

        serverLevel.addFreshEntity(ninken);
        serverLevel.sendParticles(ParticleTypes.POOF,
                ninken.getX(), ninken.getY() + 0.5, ninken.getZ(), 20, 0.4, 0.4, 0.4, 0.03);

        // The ninken heads home after two minutes
        ninjaData.scheduleDelayedTickEvent(p -> {
            if (ninken.isAlive()) {
                if (p.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.POOF,
                            ninken.getX(), ninken.getY() + 0.5, ninken.getZ(), 15, 0.3, 0.3, 0.3, 0.03);
                }
                ninken.discard();
            }
        }, DURATION_TICKS);
    }
}
