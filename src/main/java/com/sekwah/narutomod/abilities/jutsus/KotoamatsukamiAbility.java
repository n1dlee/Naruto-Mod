package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.KotoInfluenceEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class KotoamatsukamiAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant {
    private static final float COST = 150f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public boolean requiresFreeHands() { return false; }

    @Override
    public int castPoseTicks() { return 0; }

    @Override
    public long defaultCombo() { return 1211; }

    @Override
    public String requiredEye() { return "sharingan_ms"; }

    @Override
    public String requiredEyeForm() { return "shisui"; }

    @Override
    public int getCooldown() { return 300 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (!data.isSharinganActive()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.sharingan.active",
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
    public void beginInstant(ServerPlayer player, INinjaData data, BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        KotoInfluenceEvents.startPlayer(player, this, COST, onSuccess);
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (player instanceof ServerPlayer serverPlayer) {
            this.beginInstant(serverPlayer, data, (actor, state) -> {});
        }
    }
}
