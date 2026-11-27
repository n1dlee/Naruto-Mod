package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.BaikaEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class BaikaAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {
    private static final float CHAKRA_PER_TICK = 1.5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 213;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        if (!"akimichi".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.akimichi",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
            return false;
        }
        if (!BaikaEvents.canStart(player)) {
            BaikaEvents.denied(player);
            player.displayClientMessage(Component.translatable("jutsu.fail.baika_room").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int age) {
        if (!"akimichi".equals(ninjaData.getClanId()) || ninjaData.getChakra() < CHAKRA_PER_TICK || !BaikaEvents.maintainable(player)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_TICK, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int age) {
        BaikaEvents.update(player);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int age) {
        BaikaEvents.end(player);
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }
}
