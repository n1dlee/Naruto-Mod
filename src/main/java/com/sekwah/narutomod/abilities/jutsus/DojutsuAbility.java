package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class DojutsuAbility extends Ability {

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
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        Ability selected = getClanDojutsu(ninjaData);
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
        Ability selected = getClanDojutsu(ninjaData);
        if (selected == null) {
            return;
        }
        ToggleAbilityData toggles = ninjaData.getToggleAbilityData();
        ResourceLocation selectedId = NarutoRegistries.ABILITIES.getResourceKey(selected).orElseThrow().location();

        if (toggles.getAbilitiesHashSet().contains(selectedId)) {
            toggles.removeAbilityEnded(player, ninjaData, selected);
            return;
        }

        removeIfActive(player, ninjaData, NarutoAbilities.SHARINGAN.get());
        removeIfActive(player, ninjaData, NarutoAbilities.BYAKUGAN.get());
        toggles.addAbilityStarted(player, ninjaData, selected);
    }

    /**
     * Which eye this key drives. A clan's own dojutsu always wins - a Hyuga who also
     * carries a transplanted Sharingan still keys their Byakugan, and the foreign eye
     * keeps working the way it always does, passively and unclosably.
     *
     * The fallback is what matters: without it a non-Uchiha who transplanted a Sharingan
     * had no way to switch it into combat mode at all, which made the whole transplant
     * path half-dead.
     */
    private Ability getClanDojutsu(INinjaData ninjaData) {
        return switch (ninjaData.getClanId()) {
            case "uchiha" -> NarutoAbilities.SHARINGAN.get();
            case "hyuga" -> NarutoAbilities.BYAKUGAN.get();
            default -> ninjaData.isTransplantedSharingan() ? NarutoAbilities.SHARINGAN.get() : null;
        };
    }

    private void removeIfActive(Player player, INinjaData ninjaData, Ability ability) {
        ResourceLocation id = NarutoRegistries.ABILITIES.getResourceKey(ability).orElseThrow().location();
        if (ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(id)) {
            ninjaData.getToggleAbilityData().removeAbilityEnded(player, ninjaData, ability);
        }
    }
}
