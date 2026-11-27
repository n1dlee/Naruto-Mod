package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.AnimalPathRhinoEntity;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.events.SummonRitualEvents;
import com.sekwah.narutomod.util.AnimalPathDrive;
import com.sekwah.narutomod.util.SummoningCircle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class AnimalPathAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant, Ability.AlternateActivation {
    private static final float COST = 100;
    private static final SummonRitualEvents.Breed RHINO = new SummonRitualEvents.Breed(
            COST, AnimalPathDrive.WIDTH, AnimalPathDrive.HEIGHT, SummoningCircle.Style.RHINO, SummoningCircle.INSCRIBE,
            (owner, level) -> {
                if (AnimalPathRhinoEntity.owned(owner) != null) {
                    return null;
                }
                var beast = NarutoEntities.ANIMAL_RHINO.get().create(level);
                if (beast != null) {
                    beast.bond(owner);
                }
                return beast;
            });

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 7; }

    @Override
    public long defaultCombo() { return 1321; }

    @Override
    public String requiredEye() { return "rinnegan_path:animal"; }

    @Override
    public int getCooldown() { return 120 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

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
    public void beginInstant(ServerPlayer player, INinjaData data, BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        if (!SummonRitualEvents.start(player, data, onSuccess, RHINO)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.animal.space"), true);
        }
    }

    @Override
    public boolean handleAlternate(Player player, INinjaData data) {
        if (!(player instanceof ServerPlayer actor)) {
            return false;
        }
        var beast = AnimalPathRhinoEntity.owned(actor);
        if (beast == null) {
            return false;
        }
        if (actor.isShiftKeyDown()) {
            beast.dissolve();
        } else if (!beast.mark(actor)) {
            actor.displayClientMessage(Component.translatable("jutsu.fail.animal.mark"), true);
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (player instanceof ServerPlayer actor) {
            beginInstant(actor, data, (owner, state) -> {});
        }
    }
}
