package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.HiraishinEffects;
import com.sekwah.narutomod.util.HiraishinSealing;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class FlyingThunderGodAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant, Ability.AlternateActivation {
    private static final float COST = 50;

    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 3;
    }

    @Override
    public long defaultCombo() {
        return 1213;
    }

    @Override
    public int getCooldown() {
        return 40;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int chargeAmount) {
        return HiraishinEffects.canAfford(actor, data, COST, getTranslationKey(data));
    }

    @Override
    public boolean handleAlternate(Player actor, INinjaData data) {
        return actor instanceof ServerPlayer player && player.isShiftKeyDown() && HiraishinSealing.clear(player, data);
    }

    @Override
    public void beginInstant(ServerPlayer actor, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (HiraishinSealing.mark(actor, data)) {
            data.useChakra(COST, 20);
            success.accept(actor, data);
        }
    }

    @Override
    public void performServer(Player actor, INinjaData data, int ticksActive) {
        if (actor instanceof ServerPlayer player && !handleAlternate(player, data) && handleCost(player, data, ticksActive)) {
            beginInstant(player, data, (ignoredPlayer, ignoredData) -> { });
        }
    }
}
