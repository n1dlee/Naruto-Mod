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
 * Byakugan: Scouting Flight - the Hyuga stops looking and goes to see.
 *
 * The eye's radar (ByakuganEntityVisionGUI) already shows every living thing for hundreds
 * of blocks through solid rock, but only from wherever you happen to be standing. This
 * lifts the wielder out of the world's collision entirely so they can drift through walls
 * and terrain to whatever the eye picked up, which is what makes the range useful instead
 * of merely informative.
 *
 * The body genuinely travels - this is not a detached camera. Wherever the Hyuga ends up
 * is where they can be hit, and the chakra bleed is steep enough that flying off across
 * the map and back is a real commitment. Deliberately gated behind a matured eye, since
 * an academy Hyuga phasing through bedrock on day one would trivialise the whole game.
 *
 * Collision and flight are applied by PlayerEvents.reconcileKamuiPhasing, shared with
 * Kamui intangibility - see the note there on why that cannot live in this class.
 */
public class ByakuganScoutAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 4f;
    private static final int MIN_EYE_LEVEL = 2;
    private static final DustParticleOptions BYAKUGAN_PALE =
            new DustParticleOptions(new Vector3f(0.85F, 0.95F, 1.0F), 0.9F);

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 1112;
    }

    @Override
    public String requiredEye() {
        return "byakugan";
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateEyeMaturity(player, ninjaData) && validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateEyeMaturity(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Phasing and flight are reconciled centrally in PlayerEvents so both sides agree.
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 3 == 0) {
            player.level().addParticle(BYAKUGAN_PALE,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.8,
                    player.getY() + player.getRandom().nextDouble() * player.getBbHeight(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.8,
                    0.0D, 0.02D, 0.0D);
        }
    }

    private boolean validateEyeMaturity(Player player, INinjaData ninjaData) {
        if (ninjaData.getByakuganLevel() < MIN_EYE_LEVEL) {
            player.displayClientMessage(Component.translatable("jutsu.byakugan.scout.tooweak",
                    Component.literal(String.valueOf(MIN_EYE_LEVEL)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
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
