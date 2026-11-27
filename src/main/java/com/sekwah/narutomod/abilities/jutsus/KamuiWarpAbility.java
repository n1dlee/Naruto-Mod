package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.KamuiTransitEvents;
import com.sekwah.narutomod.world.KamuiDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class KamuiWarpAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant, Ability.AlternateActivation {
    private static final float COST = 120f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public boolean requiresFreeHands() { return false; }

    @Override
    public int castPoseTicks() { return 0; }

    @Override
    public long defaultCombo() { return 1313; }

    @Override
    public String requiredEye() { return "sharingan_ms"; }

    @Override
    public String requiredEyeForm() { return "obito"; }

    @Override
    public int getCooldown() { return 100; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleAlternate(Player player, INinjaData data) {
        if (player instanceof ServerPlayer server && KamuiDimension.isKamui(player.level())) {
            KamuiTransitEvents.returnPlayer(server);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() >= COST) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public void beginInstant(ServerPlayer player, INinjaData data, BiConsumer<ServerPlayer, INinjaData> confirmed) {
        KamuiTransitEvents.start(player, this, COST, confirmed);
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (player instanceof ServerPlayer serverPlayer) {
            this.beginInstant(serverPlayer, data, (actor, state) -> {});
        }
    }
}
