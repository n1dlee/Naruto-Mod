package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.HiraishinEffects;
import com.sekwah.narutomod.util.HiraishinTargeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.function.BiConsumer;

public class HiraishinTeleportAbility extends Ability implements Ability.Cooldown, Ability.DeferredInstant {
    public static final String SCROLL = "flying_thunder_god";
    private static final float COST = 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 0;
    }

    @Override
    public long defaultCombo() {
        return -1;
    }

    @Override
    public int getCooldown() {
        return 10;
    }

    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean checkLearnedRequirement(Player actor, INinjaData data) {
        if (!data.isJutsuLearned(SCROLL)) {
            HiraishinEffects.message(actor, "jutsu.fail.notlearned", Component.translatable(getTranslationKey(data)));
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int chargeAmount) {
        return HiraishinEffects.canAfford(actor, data, COST, getTranslationKey(data));
    }

    @Override
    public void beginInstant(ServerPlayer actor, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        var book = data.getHiraishinFormulas();
        if (!canJump(actor)) {
            HiraishinEffects.message(actor, "hiraishin.fail.blocked");
            return;
        }
        var landing = HiraishinTargeting.choose(actor, book);
        if (landing == null) {
            HiraishinEffects.message(actor, book.hasLegacy() ? "hiraishin.fail.legacy" : "hiraishin.fail.blocked");
            return;
        }
        Vec3 from = actor.position();
        if (from.distanceToSqr(landing.feet()) < 0.0625 || !teleport(actor, landing.feet())) {
            HiraishinEffects.message(actor, "hiraishin.fail.blocked");
            return;
        }
        data.useChakra(COST, 10);
        HiraishinEffects.jump(actor, from, landing);
        success.accept(actor, data);
    }

    @Override
    public void performServer(Player actor, INinjaData data, int ticksActive) {
        if (actor instanceof ServerPlayer player && checkLearnedRequirement(player, data) && handleCost(player, data, ticksActive)) {
            beginInstant(player, data, (ignoredPlayer, ignoredData) -> { });
        }
    }

    private static boolean canJump(ServerPlayer actor) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && !actor.isVehicle()
                && !actor.isSleeping() && !actor.isFallFlying() && !actor.noPhysics;
    }

    private static boolean teleport(ServerPlayer actor, Vec3 to) {
        ServerLevel level = actor.serverLevel();
        var event = ForgeEventFactory.onEnderTeleport(actor, to.x, to.y, to.z);
        if (event.isCanceled() || !to.equals(new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()))
                || !canJump(actor) || actor.level() != level || !HiraishinTargeting.safe(actor, to)) {
            return false;
        }
        actor.connection.teleport(to.x, to.y, to.z, actor.getYRot(), actor.getXRot());
        if (actor.position().distanceToSqr(to) > 1.0e-6) {
            return false;
        }
        actor.setDeltaMovement(Vec3.ZERO);
        actor.hurtMarked = true;
        actor.resetFallDistance();
        return true;
    }
}
