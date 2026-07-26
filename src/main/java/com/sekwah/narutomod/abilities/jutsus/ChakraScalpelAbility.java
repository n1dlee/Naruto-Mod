package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Medical Ninjutsu — Chakra Scalpel (combo 133, TOGGLE, Haruno).
 * The medic's blade-of-chakra: in surgery it heals, in combat it severs muscles and
 * tendons from the inside without breaking the skin. While toggled, the caster's melee
 * strikes cut internally — bonus damage plus Weakness on the victim (see
 * PlayerEvents.applyChakraScalpelHit for the melee hook).
 */
public class ChakraScalpelAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_PER_TICK = 0.5f;
    private static final DustParticleOptions SCALPEL_TEAL =
            new DustParticleOptions(new Vector3f(0.3F, 0.95F, 0.8F), 0.9F);

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 133;
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
        // Passive while toggled — the melee hook in PlayerEvents does the cutting
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        // Teal chakra blades sheathing both hands
        if (player.tickCount % 3 == 0) {
            double handY = player.getY() + player.getBbHeight() * 0.55;
            double yaw = Math.toRadians(player.yBodyRot);
            double sideX = Math.cos(yaw) * 0.45;
            double sideZ = Math.sin(yaw) * 0.45;
            player.level().addParticle(SCALPEL_TEAL, player.getX() + sideX, handY, player.getZ() + sideZ, 0, 0.02, 0);
            player.level().addParticle(SCALPEL_TEAL, player.getX() - sideX, handY, player.getZ() - sideZ, 0, 0.02, 0);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    private boolean validateAccess(Player player, INinjaData ninjaData) {
        if (!"haruno".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.haruno",
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
