package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.DoryuhekiCast;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class EarthWallJutsuAbility extends Ability implements Ability.Cooldown {
    private static final int CHAKRA_COST = 40;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 311;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 15f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!(player.level() instanceof ServerLevel level) || !DoryuhekiCast.reserve(level, player)) {
            player.displayClientMessage(Component.translatable("jutsu.earth_wall.no_ground"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        // The instant cost handler commits only after an eligible segment is reserved.
    }
}
