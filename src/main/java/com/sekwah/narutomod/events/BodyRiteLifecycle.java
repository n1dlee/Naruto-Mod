package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.SageModeAbility;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.s2c.BodyRitePacket.Phase;
import com.sekwah.narutomod.util.EightGateCycle;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class BodyRiteLifecycle {
    private BodyRiteLifecycle() {
    }

    @SubscribeEvent
    public static void changeWorld(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player actor = event.getEntity();
        actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            SageModeAbility.interrupt(actor, data);
            data.setSageCharge(0);
            if (data.isSageModeActive()) {
                SageModeAbility.endActive(actor, data);
            }
        });
    }

    public static void beforeNinjaMode(Player actor, INinjaData data) {
        if (!actor.isAlive()) {
            return;
        }
        if (!SageModeAbility.compatible(actor, data)) {
            SageModeAbility.interrupt(actor, data);
            data.setSageCharge(0);
            if (data.isSageModeActive()) {
                SageModeAbility.endActive(actor, data);
            }
        }
        if (data.getGateCycle().hasDeathDeadline()) {
            tickDeathGate(actor, data);
        } else if (data.getGatesOpen() > 0 && (!data.isNinjaModeEnabled() || actor.isSpectator() || data.isGiantForm())) {
            EightGatesAbility.close(actor, data, Phase.GATE_CLOSE);
        }
    }

    public static void tickSage(Player actor, INinjaData data, float upkeep) {
        if (!data.isSageModeActive() || !actor.isAlive()) {
            return;
        }
        if (data.getChakra() < upkeep || data.getSageModeTicks() <= 0) {
            SageModeAbility.endActive(actor, data);
            return;
        }
        data.useChakra(upkeep, 5);
        int ticks = data.getSageModeTicks() - 1;
        data.setSageModeTicks(ticks);
        if (ticks > 0 && actor.tickCount % 20 == 0) {
            actor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 22, 1, false, false));
            actor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 22, 0, false, false));
        }
    }

    public static void tickGates(Player actor, INinjaData data, float upkeepPerGate) {
        int stage = data.getGatesOpen();
        if (stage == 0 || !actor.isAlive()) {
            return;
        }
        boolean finalGate = stage == EightGateCycle.LAST;
        float upkeep = stage * upkeepPerGate;
        if (!finalGate && data.getStamina() < upkeep) {
            EightGatesAbility.close(actor, data, Phase.GATE_SPENT);
            return;
        }
        data.useStamina(upkeep, 5);
        int remaining = finalGate ? data.getGatesTicks() : data.getGatesTicks() - 1;
        data.setGatesTicks(remaining);
        if (remaining <= 0 && !finalGate) {
            EightGatesAbility.close(actor, data, Phase.GATE_CLOSE);
        } else if (remaining % 20 == 19) {
            pulseGates(actor, stage, finalGate);
        }
    }

    private static void tickDeathGate(Player actor, INinjaData data) {
        int remaining = data.getGateCycle().deathRemaining(BodyRiteEffects.clock(actor));
        data.setGatesTicks(remaining);
        if (remaining == 0) {
            EightGatesAbility.close(actor, data, Phase.GATE_DEATH);
            actor.hurt(actor.damageSources().genericKill(), Float.MAX_VALUE);
        } else if (remaining == 80 || remaining == 40 || remaining == 20) {
            BodyRiteEffects.emit(actor, Phase.GATE_WARNING, EightGateCycle.LAST, true);
        }
    }

    private static void pulseGates(Player actor, int stage, boolean finalGate) {
        actor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 22, finalGate ? 9 : Math.min(stage - 1, 3), false, false));
        if (stage >= 3) {
            actor.addEffect(new MobEffectInstance(MobEffects.JUMP, 22, 1, false, false));
        }
        if (finalGate) {
            actor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 22, 2, false, false));
        } else if (stage >= 5) {
            actor.hurt(actor.damageSources().magic(), 2.0f);
        }
    }
}
