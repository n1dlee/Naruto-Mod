package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KotoInfluencePacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KotoInfluence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KotoInfluenceEvents {
    private static final UUID DISTRACTION = UUID.fromString("90aca49a-d6a6-4784-a574-c7c2224ea4fd");
    private static final DustParticleOptions GLINT = new DustParticleOptions(new Vector3f(0.30f, 0.65f, 0.47f), 0.55f);
    private static final Map<LivingEntity, Focus> FOCUSES = new WeakHashMap<>();
    private static final Map<LivingEntity, Influence> INFLUENCES = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> RETRY = new WeakHashMap<>();
    private static int serial;

    private KotoInfluenceEvents() {
    }

    public static boolean affects(LivingEntity actor) {
        return !actor.level().isClientSide && INFLUENCES.containsKey(actor);
    }

    public static void startPlayer(ServerPlayer caster, Ability ability, float cost,
                                   BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        ServerLevel level = caster.serverLevel();
        if (busy(caster, level)) {
            return;
        }
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(KotoInfluence.RANGE),
                candidate -> eligible(caster, candidate)).stream()
                .min(Comparator.comparingDouble(candidate -> caster.distanceToSqr(candidate))).orElse(null);
        if (target == null) {
            miss(caster, level.getGameTime());
            return;
        }
        Predicate<LivingEntity> canPay = actor -> actor instanceof ServerPlayer player
                && player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> data.isNinjaModeEnabled()
                && data.isSharinganActive() && ability.hasEyeAccess(data) && data.getChakra() >= cost).orElse(false);
        Consumer<LivingEntity> pay = actor -> {
            if (actor instanceof ServerPlayer player) {
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                    data.useChakra(cost, 60);
                    onSuccess.accept(player, data);
                });
            }
        };
        queue(caster, target, level, new Payment(canPay, pay));
    }

    public static boolean canStartBoss(LivingEntity caster, LivingEntity target) {
        return caster.level() instanceof ServerLevel level && !busy(caster, level) && eligible(caster, target);
    }

    public static void startBoss(LivingEntity caster, LivingEntity target) {
        if (caster.level() instanceof ServerLevel level && canStartBoss(caster, target)) {
            queue(caster, target, level, new Payment(actor -> true, actor -> {}));
        }
    }

    private static boolean busy(LivingEntity actor, ServerLevel level) {
        return FOCUSES.containsKey(actor) || INFLUENCES.containsKey(actor)
                || level.getGameTime() < RETRY.getOrDefault(actor, Long.MIN_VALUE);
    }

    private static void queue(LivingEntity caster, LivingEntity target, ServerLevel level, Payment payment) {
        int id = ++serial;
        FOCUSES.put(caster, new Focus(new WeakReference<>(target), level.getGameTime() + KotoInfluence.FOCUS_TICKS, id, payment));
        Vec3 eye = caster.getEyePosition().add(direction(caster).scale(0.13));
        if (caster instanceof ServerPlayer) {
            PacketHandler.sendNear(new KotoInfluencePacket(level.dimension().location(), id, caster.getId(),
                    KotoInfluencePacket.Cue.GLINT, 0, Vec3.ZERO), level, eye, 24);
        } else {
            level.sendParticles(GLINT, eye.x, eye.y, eye.z, 3, 0.08, 0.02, 0.025, 0);
        }
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
        Influence influence = INFLUENCES.get(actor);
        if (influence != null) {
            maintain(actor, influence, now);
        }
        Focus focus = FOCUSES.get(actor);
        if (focus != null) {
            advance(level, actor, focus, now);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRetarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        Influence influence = INFLUENCES.get(mob);
        if (influence != null) {
            LivingEntity caster = influence.caster().get();
            if (validLease(mob, caster, influence, mob.level().getGameTime())) {
                event.setNewTarget(replacement(mob, caster, influence.swap()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.getAmount() <= 0) {
            return;
        }
        Influence influence = INFLUENCES.get(victim);
        if (influence != null && influence.caster().get() == event.getSource().getEntity()) {
            close(victim, true);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity actor) {
            FOCUSES.remove(actor);
            RETRY.remove(actor);
            close(actor, false);
        }
    }

    private static void advance(ServerLevel level, LivingEntity actor, Focus focus, long now) {
        LivingEntity target = focus.target().get();
        if (target == null || !eligible(actor, target) || !focus.payment().canPay().test(actor)) {
            FOCUSES.remove(actor);
            miss(actor, now);
        } else if (now >= focus.release()) {
            FOCUSES.remove(actor);
            if (apply(level, actor, target, focus.id(), now)) {
                focus.payment().pay().accept(actor);
                level.playSound(null, actor.getX(), actor.getEyeY(), actor.getZ(), NarutoSounds.KOTO_WHISPER.get(),
                        SoundSource.PLAYERS, 0.44f, 1f);
            } else {
                miss(actor, now);
            }
        }
    }

    private static boolean eligible(LivingEntity caster, LivingEntity target) {
        if (!caster.isAlive() || caster.isSpectator() || caster.hasEffect(MobEffects.BLINDNESS) || !target.isAlive()
                || target.level() != caster.level() || target.isSpectator() || target.isInvulnerable()
                || friendly(caster, target) || INFLUENCES.containsKey(target) || ShinranshinEvents.affects(target)) {
            return false;
        }
        if (target instanceof ServerPlayer player) {
            if (player.isCreative() || caster.isInvisibleTo(player)
                    || caster instanceof Player user && !user.canHarmPlayer(player)) {
                return false;
            }
        } else if (!(target instanceof Monster mob) || mob.isNoAi() || Faction.ownerOf(mob).isPresent()
                || mob.getTarget() == null || !friendly(caster, mob.getTarget())) {
            return false;
        }
        return (!(caster instanceof Player player) || !target.isInvisibleTo(player))
                && KotoInfluence.aimed(caster.getEyePosition(), direction(caster), target.getEyePosition())
                && caster.hasLineOfSight(target);
    }

    private static boolean apply(ServerLevel level, LivingEntity caster, LivingEntity target, int id, long now) {
        if (target instanceof Mob mob) {
            LivingEntity oldTarget = mob.getTarget();
            LivingEntity rival = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(10),
                    other -> isRival(caster, mob, other)).stream()
                    .min(Comparator.comparingDouble(other -> mob.distanceToSqr(other))).orElse(null);
            mob.setTarget(rival);
            if (mob.getTarget() != rival) {
                return false;
            }
            var swap = new Swap(new WeakReference<>(oldTarget), new WeakReference<>(rival));
            INFLUENCES.put(target, new Influence(new WeakReference<>(caster), now + KotoInfluence.MOB_TICKS, id, swap));
            return true;
        }
        return target instanceof ServerPlayer player && influencePlayer(caster, player, id, now);
    }

    private static boolean influencePlayer(LivingEntity caster, ServerPlayer player, int id, long now) {
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null || attack.getModifier(DISTRACTION) != null) {
            return false;
        }
        boolean perceptive = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> data.isNinjaModeEnabled()
                && (data.isByakuganActive() || data.isSharinganActive() && (data.getSharinganTomoe() >= 3 || data.isMangekyoAwakened())))
                .orElse(false);
        int ticks = KotoInfluence.playerDuration(perceptive);
        attack.addTransientModifier(new AttributeModifier(DISTRACTION, "Kotoamatsukami", KotoInfluence.ATTACK_PENALTY,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        INFLUENCES.put(player, new Influence(new WeakReference<>(caster), now + ticks, id, null));
        personal(player, id, KotoInfluencePacket.Cue.VISION, ticks, caster.position());
        player.displayClientMessage(Component.translatable("jutsu.kotoamatsukami.sensed").withStyle(ChatFormatting.DARK_GREEN), true);
        return true;
    }

    private static boolean isRival(LivingEntity caster, Mob influenced, Mob other) {
        return other != influenced && other instanceof Enemy && !(other instanceof MangekyoBossEntity)
                && !INFLUENCES.containsKey(other) && other.getTarget() != null && friendly(caster, other.getTarget())
                && validTarget(influenced, other) && !friendly(caster, other) && influenced.hasLineOfSight(other);
    }

    private static void maintain(LivingEntity target, Influence influence, long now) {
        LivingEntity caster = influence.caster().get();
        if (!validLease(target, caster, influence, now)) {
            close(target, true);
        } else if (target instanceof Mob mob) {
            LivingEntity intended = replacement(mob, caster, influence.swap());
            if (mob.getTarget() != intended) {
                mob.setTarget(intended);
                if (mob.getTarget() != intended) {
                    close(mob, true);
                }
            }
        }
    }

    private static boolean validLease(LivingEntity victim, @Nullable LivingEntity caster, Influence influence, long now) {
        return victim.isAlive() && !victim.isSpectator() && caster != null && caster.isAlive() && !caster.isRemoved()
                && KotoInfluence.maintains(now, influence.until(), caster.level() == victim.level(), caster.distanceToSqr(victim));
    }

    private static void close(LivingEntity target, boolean restore) {
        Influence influence = INFLUENCES.remove(target);
        if (influence == null) {
            return;
        }
        if (target instanceof Mob mob && influence.swap() != null) {
            Swap swap = influence.swap();
            LivingEntity oldTarget = swap.previous().get();
            if (mob.getTarget() == null || mob.getTarget() == swap.rival().get()) {
                mob.setTarget(restore && mob.isAlive() && validTarget(mob, oldTarget) ? oldTarget : null);
            }
        } else if (target instanceof ServerPlayer player) {
            var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.removeModifier(DISTRACTION);
            }
            personal(player, influence.id(), KotoInfluencePacket.Cue.END, 0, Vec3.ZERO);
        }
    }

    private static @Nullable LivingEntity replacement(Mob mob, @Nullable LivingEntity caster, @Nullable Swap swap) {
        LivingEntity rival = swap == null ? null : swap.rival().get();
        return validTarget(mob, rival) && caster != null && !friendly(caster, rival) ? rival : null;
    }

    private static boolean validTarget(Mob mob, @Nullable LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved() && target.level() == mob.level()
                && mob.distanceToSqr(target) <= 24 * 24 && !friendly(mob, target) && mob.canAttack(target);
    }

    private static boolean friendly(LivingEntity actor, LivingEntity target) {
        return actor == target || actor.isAlliedTo(target) || Faction.sameSide(actor, target);
    }

    private static Vec3 direction(LivingEntity actor) {
        return actor instanceof Player ? actor.getLookAngle() : Vec3.directionFromRotation(actor.getXRot(), actor.getYHeadRot());
    }

    private static void miss(LivingEntity caster, long now) {
        RETRY.put(caster, now + 12);
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("jutsu.kotoamatsukami.lost").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static void personal(ServerPlayer player, int id, KotoInfluencePacket.Cue cue, int duration, Vec3 origin) {
        PacketHandler.sendToPlayer(new KotoInfluencePacket(player.level().dimension().location(), id, player.getId(),
                cue, duration, origin), player);
    }

    private record Payment(Predicate<LivingEntity> canPay, Consumer<LivingEntity> pay) {
    }

    private record Focus(WeakReference<LivingEntity> target, long release, int id, Payment payment) {
    }

    private record Swap(WeakReference<LivingEntity> previous, WeakReference<LivingEntity> rival) {
    }

    private record Influence(WeakReference<LivingEntity> caster, long until, int id, @Nullable Swap swap) {
    }
}
