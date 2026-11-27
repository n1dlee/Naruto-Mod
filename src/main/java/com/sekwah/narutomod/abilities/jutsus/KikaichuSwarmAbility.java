package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.KikaichuEvents;
import com.sekwah.narutomod.util.KikaichuFlight;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class KikaichuSwarmAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {
    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 321;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData data) {
        if (!"aburame".equals(data.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.aburame",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (data.getChakra() < KikaichuFlight.COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!KikaichuEvents.queue(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.kikaichu.target"), true);
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int age) {
        return "aburame".equals(data.getClanId()) && KikaichuEvents.canMaintain(player, data);
    }

    @Override
    public void performServer(Player player, INinjaData data, int age) {
        KikaichuEvents.update(player, data);
    }

    @Override
    public void performToggleClient(Player player, INinjaData data) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData data, int age) {
        KikaichuEvents.end(player);
    }
}
