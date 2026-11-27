package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.PlayerCloneEvents;
import com.sekwah.narutomod.util.ShadowCloneFormation.Profile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class ShadowCloneJutsuAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant {
    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 8;
    }

    @Override
    public long defaultCombo() {
        return 122;
    }

    @Override
    public int getCooldown() {
        return 400;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    protected Profile profile() {
        return Profile.SQUAD;
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int chargeAmount) {
        if (data.getChakra() >= profile().cost) {
            return true;
        }
        actor.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                Component.translatable(getTranslationKey(data))).withStyle(ChatFormatting.YELLOW), true);
        return false;
    }

    @Override
    public void beginInstant(ServerPlayer actor, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        PlayerCloneEvents.start(actor, data, profile(), success);
    }

    @Override
    public void performServer(Player actor, INinjaData data, int ticksActive) {
        if (actor instanceof ServerPlayer player && handleCost(actor, data, ticksActive)) {
            beginInstant(player, data, (ignoredPlayer, ignoredData) -> { });
        }
    }
}
