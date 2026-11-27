package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.BanshoTetherEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class BanshoTeninAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant {
    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 5; }

    @Override
    public long defaultCombo() { return 1223; }

    @Override
    public String requiredEye() { return "rinnegan_path:deva"; }

    @Override
    public int getCooldown() { return 8 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() >= BanshoTetherEvents.COST) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public void beginInstant(ServerPlayer player, INinjaData data, BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        if (!BanshoTetherEvents.start(player, data, onSuccess)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.bansho.target"), true);
        }
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (player instanceof ServerPlayer server) {
            this.beginInstant(server, data, (actor, state) -> {});
        }
    }
}
