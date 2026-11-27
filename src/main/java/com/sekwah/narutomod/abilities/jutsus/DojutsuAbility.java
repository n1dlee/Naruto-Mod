package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.util.DojutsuVisual;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class DojutsuAbility extends Ability {

    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 11;
    }

    @Override
    public boolean logInChat() {
        return false;
    }

    @Override
    public net.minecraft.sounds.SoundEvent castingSound() {
        return null;
    }

    @Override
    public int castPoseTicks() {
        return 0;
    }

    @Override
    public void grantCastXp(INinjaData data) {
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        Ability selected = selectedEye(player, ninjaData);
        if (selected == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.dojutsu",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ResourceLocation selectedId = NarutoRegistries.ABILITIES.getResourceKey(selected).orElseThrow().location();
        if (ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(selectedId)) {
            return true;
        }
        if (selected instanceof Ability.ToggleStartCheck startCheck) {
            return startCheck.canStartToggle(player, ninjaData);
        }
        return selected.handleCost(player, ninjaData);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Ability selected = selectedEye(player, ninjaData);
        if (selected == null) {
            return;
        }
        ToggleAbilityData toggles = ninjaData.getToggleAbilityData();
        ResourceLocation selectedId = NarutoRegistries.ABILITIES.getResourceKey(selected).orElseThrow().location();

        if (toggles.getAbilitiesHashSet().contains(selectedId)) {
            if (toggles.removeAbilityEnded(player, ninjaData, selected)) {
                player.playNotifySound(SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.35f, 0.87f);
                show(player, ninjaData, selected, false);
            }
            return;
        }

        removeIfActive(player, ninjaData, NarutoAbilities.SHARINGAN.get());
        removeIfActive(player, ninjaData, NarutoAbilities.BYAKUGAN.get());
        if (toggles.addAbilityStarted(player, ninjaData, selected)) {
            var sound = selected.castingSound();
            if (sound != null) {
                player.level().playSound(null, player, sound, SoundSource.PLAYERS, 0.62f, 1);
            }
            show(player, ninjaData, selected, true);
        }
    }

    private static Ability selectedEye(Player player, INinjaData data) {
        String clan = data.getClanId();
        return switch (DojutsuVisual.select(clan, data.hasSharinganEye(), "hyuga".equals(clan) || data.getByakuganLevel() > 0,
                player.isShiftKeyDown())) {
            case SHARINGAN -> NarutoAbilities.SHARINGAN.get();
            case BYAKUGAN -> NarutoAbilities.BYAKUGAN.get();
            case NONE -> null;
        };
    }

    private static void show(Player player, INinjaData data, Ability eye, boolean active) {
        player.displayClientMessage(Component.translatable(active ? "jutsu.dojutsu.combat" : "jutsu.dojutsu.rest",
                        Component.translatable(eye.getTranslationKey(data)).withStyle(ChatFormatting.AQUA)),
                true);
    }

    private void removeIfActive(Player player, INinjaData ninjaData, Ability ability) {
        ResourceLocation id = NarutoRegistries.ABILITIES.getResourceKey(ability).orElseThrow().location();
        if (ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(id)) {
            ninjaData.getToggleAbilityData().removeAbilityEnded(player, ninjaData, ability);
        }
    }
}
