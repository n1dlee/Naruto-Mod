package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Kamui: Intangibility — Obito's signature Mangekyo state (combo 1212, TOGGLE).
 * The user shifts part of themselves into the Kamui dimension: attacks pass straight
 * through them. Bought with a heavy continuous chakra drain, and the immunity itself is
 * applied centrally in PlayerEvents (which reads this toggle straight off the ability set,
 * the same way Chakra Scalpel does).
 */
public class KamuiPhaseAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    /** Per-tick chakra drain — deliberately steep, this is total damage immunity. */
    private static final float CHAKRA_COST = 12f;
    private static final DustParticleOptions KAMUI_VIOLET =
            new DustParticleOptions(new Vector3f(0.45F, 0.15F, 0.65F), 1.2F);

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 1212;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ems";
    }

    @Override
    public String requiredEyeForm() {
        return "obito";
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.SHULKER_TELEPORT;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 5);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Nothing to tick server-side: the immunity is applied in PlayerEvents.livingHurt.
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 3 == 0) {
            player.level().addParticle(KAMUI_VIOLET,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.7,
                    player.getY() + player.getRandom().nextDouble() * player.getBbHeight(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.7,
                    0.0D, 0.02D, 0.0D);
        }
        if (player.tickCount % 20 == 0) {
            player.level().addParticle(NarutoParticles.SHARINGAN_RED,
                    player.getX(), player.getEyeY() - 0.1D, player.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
