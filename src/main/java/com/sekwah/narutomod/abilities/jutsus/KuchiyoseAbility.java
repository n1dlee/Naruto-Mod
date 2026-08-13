package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.SummonBeastEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kuchiyose no Jutsu — Summoning Technique (combo 312, INSTANT).
 * The classic blood-contract summon: slam a palm down and call a battle beast from its
 * own realm. The contract follows the clan — Uzumaki call the toads of Mount Myoboku,
 * Uchiha the serpents of Ryuchi Cave, Senju the slugs of Shikkotsu Forest; everyone
 * else gets a basic toad contract. Jonin+ (a summon this size takes serious chakra).
 */
public class KuchiyoseAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 100f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Bite the thumb, run the seals, slam the palm down. It should cost time. */
    @Override
    public int castPoseTicks() {
        return 26;
    }

    @Override
    public long defaultCombo() {
        return 312;
    }

    @Override
    public int getCooldown() {
        return 90 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.EVOKER_PREPARE_SUMMON;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getNinjaRank() < 3) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.jonin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
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
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SummonBeastEntity beast = NarutoEntities.SUMMON_BEAST.get().create(serverLevel);
        if (beast == null) {
            return;
        }

        byte variant;
        String contractName;
        switch (ninjaData.getClanId()) {
            case "uchiha" -> { variant = 1; contractName = "Giant Serpent"; }
            case "senju" -> { variant = 2; contractName = "Giant Slug"; }
            default -> { variant = 0; contractName = "Giant Toad"; }
        }

        Vec3 look = player.getLookAngle();
        Vec3 spawnPos = player.position().add(new Vec3(look.x, 0, look.z).normalize().scale(3.0));
        beast.setPos(spawnPos.x, player.getY(), spawnPos.z);
        beast.setOwner(player);
        beast.setVariant(variant);
        beast.setCustomName(Component.literal(contractName));
        serverLevel.addFreshEntity(beast);

        // Summoning smoke burst — the classic kuchiyose cloud
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                spawnPos.x, player.getY() + 1.0, spawnPos.z, 60, 1.5, 1.2, 1.5, 0.08);
        serverLevel.sendParticles(ParticleTypes.POOF,
                spawnPos.x, player.getY() + 0.5, spawnPos.z, 30, 1.2, 0.8, 1.2, 0.05);
    }
}
