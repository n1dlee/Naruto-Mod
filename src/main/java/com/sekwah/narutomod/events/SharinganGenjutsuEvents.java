package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SharinganGazePacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.GenjutsuGaze;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SharinganGenjutsuEvents {
    private static final UUID DISRUPTION = UUID.fromString("dabd9775-795b-4838-87a0-70fff4a8d7c9");
    private static final Map<ServerPlayer, Focus> FOCUS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> RETRY = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> DISRUPTED = new WeakHashMap<>();

    private SharinganGenjutsuEvents() {
    }

    public static void start(ServerPlayer actor, INinjaData data, Ability ability, float cost,
                             BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        ServerLevel level = actor.serverLevel();
        long now = level.getGameTime();
        if (!canFocus(actor, data, ability, cost) || FOCUS.containsKey(actor)
                || now < RETRY.getOrDefault(actor, Long.MIN_VALUE)) {
            return;
        }
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, actor.getBoundingBox().inflate(GenjutsuGaze.RANGE),
                        candidate -> GenjutsuGaze.eyesMeet(actor, candidate, GenjutsuGaze.RANGE)).stream()
                .min(Comparator.comparingDouble(candidate -> actor.getEyePosition().distanceToSqr(candidate.getEyePosition())))
                .orElse(null);
        if (target == null) {
            missed(actor);
            return;
        }
        int seed = level.random.nextInt();
        FOCUS.put(actor, new Focus(new WeakReference<>(target), now + GenjutsuGaze.FOCUS_TICKS, seed, ability, cost, onSuccess));
        nearby(actor, SharinganGazePacket.Cue.FOCUS, seed);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (!entity.isAlive() || now >= DISRUPTED.getOrDefault(entity, Long.MAX_VALUE)) {
            clear(entity);
        }
        if (entity instanceof ServerPlayer actor) {
            if (now >= RETRY.getOrDefault(actor, Long.MAX_VALUE)) {
                RETRY.remove(actor);
            }
            Focus focus = FOCUS.get(actor);
            if (focus != null) {
                actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> advance(actor, data, focus, now));
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity entity) {
            clear(entity);
            if (entity instanceof ServerPlayer actor) {
                FOCUS.remove(actor);
                RETRY.remove(actor);
            }
        }
    }

    private static void advance(ServerPlayer actor, INinjaData data, Focus focus, long now) {
        LivingEntity target = focus.target().get();
        if (!canFocus(actor, data, focus.ability(), focus.cost()) || target == null
                || !GenjutsuGaze.eyesMeet(actor, target, GenjutsuGaze.RANGE)) {
            FOCUS.remove(actor);
            nearby(actor, SharinganGazePacket.Cue.MISSED, focus.seed());
            missed(actor);
        } else if (now >= focus.release()) {
            FOCUS.remove(actor);
            data.useChakra(focus.cost(), 40);
            nearby(actor, SharinganGazePacket.Cue.LOCKED, focus.seed());
            apply(target, now, focus.seed());
            focus.onSuccess().accept(actor, data);
        }
    }

    private static boolean canFocus(ServerPlayer actor, INinjaData data, Ability jutsu, float cost) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isSleeping() && !actor.hasEffect(MobEffects.BLINDNESS)
                && data.isNinjaModeEnabled() && data.isSharinganActive() && jutsu.hasEyeAccess(data) && data.getChakra() >= cost;
    }

    private static void apply(LivingEntity target, long now, int seed) {
        int tomoe = target.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> {
            if (!data.isNinjaModeEnabled() || !data.isSharinganActive()) {
                return 0;
            }
            return data.isMangekyoAwakened() ? 3 : data.getSharinganTomoe();
        }).orElse(0);
        int duration = GenjutsuGaze.duration(tomoe);
        if (duration == 0) {
            if (target instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("sharingan.genjutsu.broken").withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        DISRUPTED.merge(target, now + duration, Math::max);
        modifier(target, Attributes.MOVEMENT_SPEED, -0.27);
        modifier(target, Attributes.ATTACK_DAMAGE, -0.23);
        if (target instanceof ServerPlayer player) {
            int remainder = (int) (DISRUPTED.get(target) - now);
            PacketHandler.sendToPlayer(new SharinganGazePacket(player.level().dimension().location(), player.getId(),
                    SharinganGazePacket.Cue.VICTIM, remainder, seed), player);
            player.playNotifySound(NarutoSounds.SHARINGAN_WHISPER.get(), SoundSource.PLAYERS, 0.67f, 1.0f);
            player.displayClientMessage(Component.translatable("jutsu.sharingan_genjutsu.affected")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    private static void modifier(LivingEntity entity, Attribute attribute, double penalty) {
        var instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(DISRUPTION) == null) {
            instance.addTransientModifier(new AttributeModifier(DISRUPTION, "Sharingan illusion", penalty,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void clear(LivingEntity entity) {
        if (DISRUPTED.remove(entity) == null) {
            return;
        }
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (speed != null) {
            speed.removeModifier(DISRUPTION);
        }
        if (attack != null) {
            attack.removeModifier(DISRUPTION);
        }
    }

    private static void nearby(ServerPlayer actor, SharinganGazePacket.Cue cue, int seed) {
        PacketHandler.sendNear(new SharinganGazePacket(actor.level().dimension().location(), actor.getId(), cue, 0, seed),
                actor.serverLevel(), actor.position(), 28);
    }

    private static void missed(ServerPlayer actor) {
        RETRY.put(actor, actor.serverLevel().getGameTime() + 10);
        actor.displayClientMessage(Component.translatable("jutsu.sharingan_genjutsu.no_contact")
                .withStyle(ChatFormatting.GRAY), true);
    }

    private record Focus(WeakReference<LivingEntity> target, long release, int seed, Ability ability, float cost,
                         BiConsumer<ServerPlayer, INinjaData> onSuccess) {
    }
}
