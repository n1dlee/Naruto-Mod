package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ShadowGripPacket;
import com.sekwah.narutomod.network.s2c.ShadowGripPacket.Step;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ShadowGrip;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowGripEvents {
    private static final Map<LivingEntity, Grip> ACTIVE = new WeakHashMap<>();
    private static int serial;

    private ShadowGripEvents() {
    }

    public static boolean canStart(LivingEntity caster) {
        var link = ShadowTetherEvents.link(caster);
        return link != null && caster.isAlive() && !caster.noPhysics && !ACTIVE.containsKey(caster)
                && ShadowTetherEvents.recheck(caster, link);
    }

    public static void start(LivingEntity caster, float damage) {
        var link = ShadowTetherEvents.link(caster);
        if (!(caster.level() instanceof ServerLevel level) || link == null || !canStart(caster)) {
            return;
        }
        var grip = new Grip(++serial, link.id(), level, link.target(), damage);
        ACTIVE.put(caster, grip);
        visual(grip, Step.LIFT);
        sound(grip, NarutoSounds.SHADOW_GRIP_LIFT.get(), 0.59f);
    }

    public static void followBoss(LivingEntity actor) {
        if (actor instanceof MangekyoBossEntity boss && boss.getRandom().nextInt(3) == 0
                && canStart(actor) && boss.useChakra(36f)) {
            start(boss, 3.7f);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity actor = event.getEntity();
        if (actor.level().isClientSide) {
            return;
        }
        Grip grip = ACTIVE.get(actor);
        if (grip == null) {
            return;
        }
        LivingEntity victim = grip.victim.get();
        var link = ShadowTetherEvents.link(actor);
        int age = (int) (grip.level.getGameTime() - grip.started);
        if (victim == null || !actor.isAlive() || actor.noPhysics || actor.level() != grip.level || link == null
                || !ShadowGrip.matches(grip.binding, link.id(), victim == link.target()) || age >= ShadowGrip.duration()) {
            stop(actor, grip);
            return;
        }
        grip.feet = victim.position();
        if (ShadowGrip.pulseAt(age) >= 0) {
            if (!ShadowTetherEvents.recheck(actor, link)) {
                stop(actor, grip);
                return;
            }
            pulse(actor, victim, grip);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity actor) {
            Grip grip = ACTIVE.get(actor);
            if (grip != null) {
                stop(actor, grip);
            }
        }
    }

    private static void pulse(LivingEntity actor, LivingEntity victim, Grip grip) {
        float damage = Math.min(grip.damage, victim instanceof Player ? 4.0f : 10.5f);
        boolean accepted = victim.hurt(actor.damageSources().indirectMagic(actor, actor), damage);
        visual(grip, accepted ? Step.CLENCH : Step.HOLD);
        if (accepted) {
            sound(grip, NarutoSounds.SHADOW_GRIP_CLENCH.get(), 0.68f);
        }
    }

    private static void stop(LivingEntity caster, Grip grip) {
        ACTIVE.remove(caster);
        visual(grip, Step.DROP);
        sound(grip, NarutoSounds.SHADOW_GRIP_DROP.get(), 0.49f);
    }

    private static void visual(Grip grip, Step step) {
        PacketHandler.sendNear(new ShadowGripPacket(grip.level.dimension().location(), grip.id, grip.targetId,
                step, grip.feet, grip.height), grip.level, grip.feet, 38);
    }

    private static void sound(Grip grip, net.minecraft.sounds.SoundEvent audio, float volume) {
        grip.level.playSound(null, BlockPos.containing(grip.feet.add(0, grip.height * 0.72, 0)), audio,
                SoundSource.PLAYERS, volume, 1f);
    }

    private static final class Grip {
        private final int id;
        private final int binding;
        private final ServerLevel level;
        private final long started;
        private final WeakReference<LivingEntity> victim;
        private final int targetId;
        private final float height;
        private final float damage;
        private Vec3 feet;

        private Grip(int id, int binding, ServerLevel level, LivingEntity victim, float damage) {
            this.id = id;
            this.binding = binding;
            this.level = level;
            this.started = level.getGameTime();
            this.victim = new WeakReference<>(victim);
            this.targetId = victim.getId();
            this.height = victim.getBbHeight();
            this.damage = damage;
            this.feet = victim.position();
        }
    }
}
