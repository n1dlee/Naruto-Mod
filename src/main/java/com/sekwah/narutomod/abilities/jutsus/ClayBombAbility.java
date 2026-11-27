package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.ClayBombEntity;
import com.sekwah.narutomod.events.ClayChargeEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ClayBombAbility extends Ability implements Ability.Cooldown, Ability.AlternateActivation {
    private static final float CHAKRA_COST = 75f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 12; }

    @Override
    public long defaultCombo() { return 2321; }

    @Override
    public String element() { return "earth"; }

    @Override
    public int elementLevelRequired() { return 8; }

    @Override
    public String secondaryElement() { return "fire"; }

    @Override
    public int secondaryElementLevelRequired() { return 8; }

    @Override
    public int getCooldown() { return 12 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleAlternate(Player player, INinjaData ninjaData) {
        return ClayChargeEvents.detonateExisting(player);
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!ClayBombEntity.launch(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.clay.space"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }
}
