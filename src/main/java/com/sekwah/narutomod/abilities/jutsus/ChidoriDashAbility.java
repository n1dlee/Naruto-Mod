package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.ChidoriDashEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ChidoriDashAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 40.0F;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return ChidoriDashEvents.PLAYER_WINDUP;
    }

    @Override
    public long defaultCombo() {
        return 2121;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 9;
    }

    @Override
    public float elementXpReward() {
        return 30f;
    }

    @Override
    public HandsBusy builtOn() {
        return HandsBusy.CHIDORI;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.isWallWalkAttached() || !ChidoriDashEvents.canStart(player)) {
            player.displayClientMessage(Component.translatable("jutsu.chidori_dash.invalid_stance"), true);
            return false;
        }
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
        ninjaData.setChidoriTicks(0);
        float scale = ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
        ChidoriDashEvents.start(player, player.getLookAngle(),
                new ChidoriDashEvents.DamageProfile(22f * scale, 16f * scale, ninjaData.getNinjaRank() < 4, false));
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }
}
