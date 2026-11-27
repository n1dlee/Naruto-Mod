package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.SummonBeastVariant;
import com.sekwah.narutomod.events.ContractSummonEvents;
import com.sekwah.narutomod.events.SummonRitualEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class KuchiyoseAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant, Ability.AlternateActivation {
    private static final float COST = 100;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 10; }

    @Override
    public long defaultCombo() { return 312; }

    @Override
    public int getCooldown() { return 90 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getNinjaRank() < 3) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.jonin",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (data.getChakra() < COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    @Override
    public void beginInstant(ServerPlayer player, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (ContractSummonEvents.occupied(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.summon.active"), true);
            return;
        }
        var variant = SummonBeastVariant.forNinja(data.getSummonContract(), data.getClanId());
        var breed = new SummonRitualEvents.Breed(COST, variant.getWidth(), variant.getHeight(), variant.sealStyle(), 9,
                (owner, level) -> {
                    if (ContractSummonEvents.occupied(owner)) {
                        return null;
                    }
                    var mob = NarutoEntities.SUMMON_BEAST.get().create(level);
                    if (mob == null) {
                        return null;
                    }
                    mob.setOwner(owner);
                    mob.setVariant(variant);
                    mob.setHealth(mob.getMaxHealth());
                    mob.setCustomName(Component.literal(variant.getDisplayName()));
                    mob.setCustomNameVisible(true);
                    return mob;
                });
        if (!SummonRitualEvents.start(player, data, success, breed)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.summon.space"), true);
        }
    }

    @Override
    public boolean handleAlternate(Player player, INinjaData data) {
        return player instanceof ServerPlayer owner && owner.isShiftKeyDown() && ContractSummonEvents.recall(owner);
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (player instanceof ServerPlayer owner) {
            beginInstant(owner, data, (actor, state) -> {});
        }
    }
}
