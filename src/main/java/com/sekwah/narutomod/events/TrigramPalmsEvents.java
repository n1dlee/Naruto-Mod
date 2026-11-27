package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.TrigramPalmsPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.TrigramSequence;
import com.sekwah.narutomod.util.TrigramSequence.Cue;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrigramPalmsEvents {
    private static final Map<LivingEntity, Series> RUNNING = new WeakHashMap<>();
    private static final Set<LivingEntity> PULSE_CONTACTS = new HashSet<>();
    private static int serial;

    private TrigramPalmsEvents() {
    }

    public static boolean running(LivingEntity actor) {
        return !actor.level().isClientSide && RUNNING.containsKey(actor);
    }

    public static @Nullable LivingEntity select(Player player) {
        if (!casterReady(player)) {
            return null;
        }
        Vec3 yaw = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(TrigramSequence.START_RANGE),
                        target -> canStart(player, target) && chest(target).subtract(eye).normalize().dot(yaw) >= 0.56)
                .stream().min(Comparator.comparingDouble(target -> chest(target).distanceToSqr(eye))).orElse(null);
    }

    public static boolean canStart(LivingEntity actor, LivingEntity target) {
        return casterReady(actor) && eligible(actor, target) && actor.distanceToSqr(target) <= TrigramSequence.START_RANGE * TrigramSequence.START_RANGE
                && TrigramSequence.within(actor.position(), actor.position(), target.position()) && actor.hasLineOfSight(target);
    }

    private static boolean casterReady(LivingEntity actor) {
        return actor.level() instanceof ServerLevel && actor.isAlive() && actor.onGround() && !actor.noPhysics
                && !actor.isSpectator() && !actor.isPassenger() && !running(actor) && !AdamantineChainEvents.sealed(actor)
                && !KaitenEvents.spinning(actor);
    }

    public static void start(LivingEntity actor, LivingEntity target, float rank) {
        if (!(actor.level() instanceof ServerLevel level) || !canStart(actor, target)) {
            return;
        }
        var series = new Series(level, ++serial, actor, target, rank);
        RUNNING.put(actor, series);
        visual(actor, series, Cue.STANCE);
        level.playSound(null, actor.blockPosition(), NarutoSounds.TRIGRAM_FIELD.get(), SoundSource.PLAYERS, 0.67f, 1f);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity actor = event.getEntity();
        if (actor.level().isClientSide) {
            return;
        }
        Series cast = RUNNING.get(actor);
        if (cast == null) {
            return;
        }
        LivingEntity target = cast.target.get();
        if (target == null || !valid(actor, cast, target)) {
            cancel(actor, cast);
            return;
        }
        if (actor instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target, 35, 35);
        }
        cast.contact = chest(target);
        if (cast.level.getGameTime() - cast.start >= TrigramSequence.beat(cast.phase)) {
            strike(actor, target, cast);
        }
    }

    private static boolean valid(LivingEntity actor, Series cast, LivingEntity target) {
        return actor.level() == cast.level && actor.isAlive() && !actor.isSpectator() && !actor.noPhysics
                && !actor.isPassenger() && !AdamantineChainEvents.sealed(actor) && eligible(actor, target)
                && TrigramSequence.within(cast.anchor, actor.position(), target.position()) && actor.hasLineOfSight(target)
                && (!(actor instanceof Player player) || player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isByakuganActive()).orElse(false));
    }

    private static void strike(LivingEntity actor, LivingEntity target, Series cast) {
        var source = NarutoDamageTypes.getDamageSource(cast.level, NarutoDamageTypes.TRIGRAM_PALMS, actor);
        boolean accepted;
        PULSE_CONTACTS.add(target);
        try {
            accepted = target.hurt(source, TrigramSequence.damage(cast.phase, cast.rank, target instanceof Player));
        } finally {
            PULSE_CONTACTS.remove(target);
        }
        if (!accepted) {
            cancel(actor, cast);
            return;
        }
        boolean finalBeat = TrigramSequence.finalBeat(cast.phase, accepted);
        if (actor instanceof Player player) {
            player.displayClientMessage(Component.translatable("jutsu.trigram.count", TrigramSequence.total(cast.phase))
                    .withStyle(finalBeat ? ChatFormatting.GOLD : ChatFormatting.WHITE), true);
        }
        visual(actor, cast, finalBeat ? Cue.SEALED : Cue.BURST);
        var sound = finalBeat ? NarutoSounds.TRIGRAM_SEAL.get() : NarutoSounds.trigramBeat(cast.phase);
        cast.level.playSound(null, cast.contact.x, cast.contact.y, cast.contact.z, sound, SoundSource.PLAYERS, 0.78f, 1);
        if (finalBeat) {
            seal(actor, target);
            RUNNING.remove(actor);
        } else {
            cast.phase++;
        }
    }

    private static void seal(LivingEntity actor, LivingEntity target) {
        if (!target.isAlive()) {
            return;
        }
        boolean player = target instanceof Player;
        int ticks = player ? 32 : 50;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, false, true));
        Vec3 away = target.position().subtract(actor.position());
        target.knockback(1.08, -away.x, -away.z);
        target.hurtMarked = true;
        target.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> data.useChakra(Math.min(55, data.getChakra() * 0.20f), ticks));
    }

    private static void cancel(LivingEntity actor, Series cast) {
        visual(actor, cast, Cue.BREAK);
        RUNNING.remove(actor);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity actor) {
            Series cast = RUNNING.get(actor);
            if (cast != null) {
                cancel(actor, cast);
            }
        }
    }

    @SubscribeEvent
    public static void preventNormalAttack(AttackEntityEvent event) {
        if (running(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void stayInReach(LivingKnockBackEvent event) {
        if (!event.getEntity().level().isClientSide && PULSE_CONTACTS.contains(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean eligible(LivingEntity actor, LivingEntity target) {
        return target != actor && target.level() == actor.level() && target.isAlive() && target.isPickable() && !target.noPhysics
                && !target.isSpectator() && !actor.isAlliedTo(target) && !Faction.sameSide(actor, target)
                && !(target instanceof TamableAnimal tame && tame.isOwnedBy(actor))
                && (!(target instanceof Player player) || !player.isCreative()
                && (!(actor instanceof Player human) || human.canHarmPlayer(player)));
    }

    private static Vec3 chest(LivingEntity actor) {
        return actor.position().add(0, actor.getBbHeight() * 0.56, 0);
    }

    private static void visual(LivingEntity actor, Series cast, Cue cue) {
        PacketHandler.sendNear(new TrigramPalmsPacket(cast.level.dimension().location(), cast.id, actor.getId(), cast.targetId,
                cue, cast.phase, cast.anchor, cast.contact), cast.level, cast.anchor, 36);
    }

    private static final class Series {
        private final ServerLevel level;
        private final int id;
        private final long start;
        private final Vec3 anchor;
        private final WeakReference<LivingEntity> target;
        private final int targetId;
        private final float rank;
        private Vec3 contact;
        private int phase;

        private Series(ServerLevel level, int id, LivingEntity actor, LivingEntity target, float rank) {
            this.level = level;
            this.id = id;
            this.start = level.getGameTime();
            this.anchor = actor.position();
            this.target = new WeakReference<>(target);
            this.targetId = target.getId();
            this.rank = rank;
            this.contact = chest(target);
        }
    }
}
