package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.EarthSpikeCast;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.SpikeField;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class EarthSpikesAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 30f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 313;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.EARTH_SPIKE_CRACK.get();
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 4;
    }

    @Override
    public float elementXpReward() {
        return 20f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        int level = data.getElementLevel("earth");
        if (!EarthSpikeCast.disc(player, SpikeField.radiusFor(level), SpikeField.countFor(level),
                8 * data.getRankDamageMultiplier())) {
            player.displayClientMessage(Component.translatable("jutsu.earth_spikes.no_ground"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        // The instant cost handler applies the field and spends chakra only if a column was accepted.
    }
}
