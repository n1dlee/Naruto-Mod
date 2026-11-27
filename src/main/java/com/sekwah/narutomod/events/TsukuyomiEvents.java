package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SharinganGazePacket;
import com.sekwah.narutomod.network.s2c.TsukuyomiPacket;
import com.sekwah.narutomod.util.GenjutsuGaze;
import com.sekwah.narutomod.util.NarutoParticles;
import com.sekwah.narutomod.util.TsukuyomiScene;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TsukuyomiEvents {
    private static final UUID HOLD = UUID.fromString("2d580c4e-091f-45bd-91c9-bf8c57a06255");
    private static final Map<LivingEntity, Focus> FOCUS = new WeakHashMap<>();
    private static final Map<LivingEntity, Scene> SCENES = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> RETRY = new WeakHashMap<>();
    private static int sceneId;

    private TsukuyomiEvents() {
    }

    public static void startPlayer(ServerPlayer actor, INinjaData data, Ability ability, float cost,
                                   BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        ServerLevel level = actor.serverLevel();
        if (busy(actor, level) || !ability.hasEyeAccess(data)) {
            return;
        }
        var target = level.getEntitiesOfClass(LivingEntity.class, actor.getBoundingBox().inflate(TsukuyomiScene.RANGE),
                        candidate -> available(actor, candidate)).stream()
                .min(Comparator.comparingDouble(candidate -> actor.distanceToSqr(candidate))).orElse(null);
        if (target == null) {
            miss(actor, level);
            return;
        }
        queue(actor, target, level, TsukuyomiScene.PLAYER_FOCUS, 10f * data.getRankDamageMultiplier(),
                caster -> caster instanceof ServerPlayer player && player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                        .map(current -> pay(player, current, ability, cost, onSuccess)).orElse(false));
    }

    public static boolean canStartBoss(LivingEntity boss, LivingEntity target) {
        return boss.level() instanceof ServerLevel level && !busy(boss, level) && available(boss, target);
    }

    public static void startBoss(LivingEntity boss, LivingEntity target) {
        if (boss.level() instanceof ServerLevel level && !busy(boss, level) && available(boss, target)) {
            queue(boss, target, level, TsukuyomiScene.BOSS_FOCUS, 6f, actor -> true);
        }
    }

    private static boolean pay(ServerPlayer player, INinjaData data, Ability ability, float cost,
                               BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        if (!data.isNinjaModeEnabled() || !ability.hasEyeAccess(data) || data.getChakra() < cost) {
            return false;
        }
        data.useChakra(cost, 40);
        onSuccess.accept(player, data);
        return true;
    }

    private static boolean busy(LivingEntity actor, ServerLevel level) {
        return FOCUS.containsKey(actor) || SCENES.containsKey(actor)
                || level.getGameTime() < RETRY.getOrDefault(actor, Long.MIN_VALUE);
    }

    private static boolean available(LivingEntity actor, LivingEntity victim) {
        return !SCENES.containsKey(victim) && GenjutsuGaze.eyesMeet(actor, victim, TsukuyomiScene.RANGE);
    }

    private static void queue(LivingEntity actor, LivingEntity target, ServerLevel level, int ticks, float damage,
                              Predicate<LivingEntity> commit) {
        int id = ++sceneId;
        FOCUS.put(actor, new Focus(new WeakReference<>(target), level.getGameTime() + ticks, id, damage, commit));
        glimpse(actor, level, SharinganGazePacket.Cue.FOCUS, id);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity actor = event.getEntity();
        if (!(actor.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now >= RETRY.getOrDefault(actor, Long.MAX_VALUE)) {
            RETRY.remove(actor);
        }
        Scene scene = SCENES.get(actor);
        if (scene != null && expired(actor, scene, now)) {
            close(actor);
        }
        Focus focus = FOCUS.get(actor);
        if (focus != null) {
            advance(actor, level, now, focus);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getAmount() > 0) {
            close(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity entity) {
            close(entity);
            FOCUS.remove(entity);
            RETRY.remove(entity);
        }
    }

    private static void advance(LivingEntity actor, ServerLevel level, long now, Focus focus) {
        LivingEntity target = focus.target().get();
        if (target == null || !available(actor, target)) {
            FOCUS.remove(actor);
            glimpse(actor, level, SharinganGazePacket.Cue.MISSED, focus.id());
            miss(actor, level);
        } else if (now >= focus.release()) {
            FOCUS.remove(actor);
            if (focus.commit().test(actor)) {
                glimpse(actor, level, SharinganGazePacket.Cue.LOCKED, focus.id());
                apply(actor, target, now, focus);
            }
        }
    }

    private static void apply(LivingEntity actor, LivingEntity target, long now, Focus focus) {
        int active = target.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .filter(data -> data.isNinjaModeEnabled() && data.isSharinganActive())
                .map(data -> data.isMangekyoAwakened() ? 3 : data.getSharinganTomoe()).orElse(0);
        var source = actor.damageSources().indirectMagic(actor, actor);
        if (!target.hurt(source, focus.damage() * (active >= 3 ? 0.72f : 1f)) || !target.isAlive()) {
            return;
        }
        int duration = TsukuyomiScene.duration(active);
        SCENES.put(target, new Scene(focus.id(), now + duration, new WeakReference<>(actor)));
        var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(HOLD) == null) {
            speed.addTransientModifier(new AttributeModifier(HOLD, "Tsukuyomi", -0.56, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (target instanceof ServerPlayer player) {
            send(player, focus.id(), duration);
        }
    }

    private static boolean expired(LivingEntity actor, Scene scene, long now) {
        LivingEntity caster = scene.caster().get();
        return now >= scene.until() || !actor.isAlive() || caster == null || !caster.isAlive()
                || caster.isRemoved() || caster.level() != actor.level();
    }

    private static void close(LivingEntity entity) {
        Scene scene = SCENES.remove(entity);
        if (scene == null) {
            return;
        }
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(HOLD);
        }
        if (entity instanceof ServerPlayer player) {
            send(player, scene.id(), 0);
        }
    }

    private static void send(ServerPlayer victim, int id, int duration) {
        PacketHandler.sendToPlayer(new TsukuyomiPacket(victim.level().dimension().location(), victim.getId(), id, duration), victim);
    }

    private static void miss(LivingEntity actor, ServerLevel level) {
        RETRY.put(actor, level.getGameTime() + 12);
        if (actor instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("jutsu.sharingan_genjutsu.no_contact")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static void glimpse(LivingEntity actor, ServerLevel level, SharinganGazePacket.Cue cue, int id) {
        if (actor instanceof ServerPlayer player) {
            PacketHandler.sendNear(new SharinganGazePacket(level.dimension().location(), player.getId(), cue, 0, id),
                    level, actor.position(), 28);
        } else if (cue != SharinganGazePacket.Cue.MISSED) {
            var eyes = actor.getEyePosition().add(actor.getLookAngle().scale(0.21));
            level.sendParticles(NarutoParticles.GENJUTSU_RED, eyes.x, eyes.y, eyes.z, 5, 0.11, 0.025, 0.06, 0.0);
        }
    }

    private record Focus(WeakReference<LivingEntity> target, long release, int id, float damage,
                         Predicate<LivingEntity> commit) {
    }

    private record Scene(int id, long until, WeakReference<LivingEntity> caster) {
    }
}
