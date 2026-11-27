package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.WoodHouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class WoodHouseAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant {
    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 8; }

    @Override
    public long defaultCombo() { return 3221; }

    @Override
    public String requiredClan() { return "senju"; }

    @Override
    public String element() { return "earth"; }

    @Override
    public int elementLevelRequired() { return 10; }

    @Override
    public String secondaryElement() { return "water"; }

    @Override
    public int secondaryElementLevelRequired() { return 10; }

    @Override
    public int getCooldown() { return 120 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() >= WoodHouseEvents.COST) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public void beginInstant(ServerPlayer player, INinjaData data, BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        WoodHouseEvents.start(player, data, onSuccess);
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (player instanceof ServerPlayer server) {
            this.beginInstant(server, data, (actor, state) -> {});
        }
    }
}
