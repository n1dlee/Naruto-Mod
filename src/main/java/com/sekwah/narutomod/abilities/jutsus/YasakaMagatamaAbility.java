package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.YasakaVolleyEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class YasakaMagatamaAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant {
    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1331;
    }

    @Override
    public int getCooldown() {
        return 100;
    }

    @Override
    public int castPoseTicks() {
        return 8;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int age) {
        if (!data.isSusanooActive() || data.getSusanooStage() < 2) {
            actor.displayClientMessage(Component.translatable("yasaka.requires_arm").withStyle(ChatFormatting.DARK_PURPLE), true);
            return false;
        }
        if (data.getChakra() < YasakaVolleyEvents.COST) {
            actor.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(getTranslationKey(data))).withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        return true;
    }

    @Override
    public void beginInstant(ServerPlayer actor, INinjaData data, BiConsumer<ServerPlayer, INinjaData> commit) {
        YasakaVolleyEvents.start(actor, data, commit);
    }

    @Override
    public void performServer(Player actor, INinjaData data, int age) {
        if (actor instanceof ServerPlayer player && handleCost(player, data, age)) {
            beginInstant(player, data, (ignored, state) -> { });
        }
    }
}
