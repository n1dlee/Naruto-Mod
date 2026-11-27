package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ShinranshinPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.Shinranshin;
import com.sekwah.narutomod.util.Shinranshin.Stage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShinranshinEvents {
    private static final Map<Player, Lease> CASTS = new WeakHashMap<>();
    private static final Map<LivingEntity, Lease> SUBJECTS = new WeakHashMap<>();
    private static final UUID HESITATION = UUID.fromString("5f2b8443-3761-4b08-be3d-1265d87b3f57");
    private static int serial;

    private ShinranshinEvents() {
    }

    public static @Nullable LivingEntity select(Player caster) {
        return !CASTS.containsKey(caster) && !affects(caster) ? JutsuAim.select(caster, Shinranshin.RANGE, 18,
                target -> eligible(target) && !SUBJECTS.containsKey(target) && !KotoInfluenceEvents.affects(target)) : null;
    }

    public static boolean affects(LivingEntity actor) {
        return !actor.level().isClientSide && SUBJECTS.containsKey(actor);
    }

    public static void start(Player caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel level) || CASTS.containsKey(caster) || SUBJECTS.containsKey(target)) {
            return;
        }
        var lease = new Lease(level, ++serial, target, caster.getEyePosition());
        CASTS.put(caster, lease);
        SUBJECTS.put(target, lease);
        send(caster, lease, Stage.WINDUP, Shinranshin.FOCUS);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player caster) || caster.level().isClientSide) {
            return;
        }
        Lease lease = CASTS.get(caster);
        if (lease == null) {
            return;
        }
        LivingEntity target = lease.target.get();
        long now = lease.level.getGameTime();
        if (!valid(caster, target, lease, now)) {
            close(caster, lease);
            return;
        }
        lease.contact = JutsuAim.center(target);
        if (!lease.installed && now >= lease.opens) {
            int duration = target instanceof Player ? Shinranshin.PVP_TICKS : Shinranshin.MOB_TICKS;
            if (!apply(caster, target, lease)) {
                close(caster, lease);
                return;
            }
            lease.installed = true;
            lease.expires = now + duration;
            send(caster, lease, Stage.GRIP, duration);
            lease.level.playSound(null, lease.contact.x, lease.contact.y, lease.contact.z,
                    NarutoSounds.SHINRANSHIN_GRIP.get(), SoundSource.PLAYERS, 0.69f, 1);
        } else if (lease.installed && (lease.expires - now) % 8 == 0) {
            send(caster, lease, Stage.GRIP, (int) (lease.expires - now));
        }
    }

    private static boolean valid(Player caster, @Nullable LivingEntity target, Lease lease, long now) {
        if (target == null || !caster.isAlive() || caster.level() != lease.level || caster.isSpectator() || caster.noPhysics
                || !JutsuAim.attackable(caster, target)) {
            return false;
        }
        boolean open = caster.hasLineOfSight(target);
        if (!lease.installed) {
            return open && JutsuAim.aimed(caster.getEyePosition(), caster.getLookAngle(), JutsuAim.center(target),
                    Shinranshin.RANGE, Math.cos(Math.toRadians(22)));
        }
        if (lease.swap != null && lease.swap.assigned != null && !available(lease.swap.reference.get(), lease.level)) {
            return false;
        }
        return Shinranshin.maintains(now, lease.expires, caster.distanceToSqr(target), open);
    }

    private static boolean eligible(LivingEntity target) {
        return target instanceof Player || target instanceof Monster mob
                && !mob.isNoAi() && mob.getTarget() != null && Faction.ownerOf(mob).isEmpty();
    }

    private static boolean apply(Player caster, LivingEntity target, Lease lease) {
        if (target instanceof Player player) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null || speed.getModifier(HESITATION) != null) {
                return false;
            }
            speed.addTransientModifier(new AttributeModifier(HESITATION, "Shinranshin hesitation", -0.21,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jutsu.shinranshin.affected"), true);
            return true;
        }
        if (!(target instanceof Monster mob)) {
            return false;
        }
        LivingEntity old = mob.getTarget();
        LivingEntity rival = lease.level.getEntitiesOfClass(Monster.class, mob.getBoundingBox().inflate(6),
                other -> other != mob && other != old
                        && !SUBJECTS.containsKey(other) && Faction.ownerOf(other).isEmpty()
                        && JutsuAim.attackable(caster, other) && mob.canAttack(other) && mob.hasLineOfSight(other))
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        mob.setTarget(rival);
        if (mob.getTarget() != rival) {
            return false;
        }
        lease.swap = new Swap(old, rival);
        if (rival == null) {
            mob.getNavigation().stop();
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void holdTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Monster mob) || mob.level().isClientSide) {
            return;
        }
        Lease lease = SUBJECTS.get(mob);
        if (lease != null && lease.installed && lease.swap != null && lease.level.getGameTime() < lease.expires) {
            var forced = lease.swap.reference.get();
            if (lease.swap.assigned == null || available(forced, lease.level)) {
                event.setNewTarget(forced);
            }
        }
    }

    @SubscribeEvent
    public static void interrupt(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getAmount() > 0
                && event.getEntity() instanceof Player caster) {
            Lease lease = CASTS.get(caster);
            if (lease != null) {
                close(caster, lease);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity actor)) {
            return;
        }
        if (actor instanceof Player player && CASTS.containsKey(player)) {
            close(player, CASTS.get(player));
        }
        Lease lease = SUBJECTS.get(actor);
        if (lease != null) {
            for (var entry : CASTS.entrySet()) {
                if (entry.getValue() == lease) {
                    close(entry.getKey(), lease);
                    return;
                }
            }
        }
    }

    private static void close(Player caster, Lease lease) {
        CASTS.remove(caster);
        LivingEntity target = lease.target.get();
        if (target != null && SUBJECTS.remove(target, lease) && lease.installed) {
            if (target instanceof Player player) {
                var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(HESITATION);
                }
            } else if (target instanceof Monster mob && lease.swap != null) {
                restore(mob, lease);
            }
        }
        send(caster, lease, Stage.BREAK, Shinranshin.FADE);
    }

    private static void restore(Monster mob, Lease lease) {
        var swap = lease.swap;
        var current = mob.getTarget();
        var old = swap.previous.get();
        boolean lost = swap.assigned != null && swap.reference.get() == null;
        if (Shinranshin.ownsTarget(swap.assigned, current == null ? null : current.getUUID(), lost)) {
            mob.setTarget(available(old, lease.level) ? old : null);
            mob.getNavigation().stop();
        }
    }

    private static boolean available(@Nullable LivingEntity target, ServerLevel level) {
        return target != null && target.isAlive() && !target.isRemoved() && target.level() == level;
    }

    private static void send(Player caster, Lease lease, Stage stage, int remaining) {
        if (caster.level() == lease.level) {
            lease.origin = caster.getEyePosition().add(caster.getLookAngle().scale(0.25)).add(0, -0.25, 0);
        }
        PacketHandler.sendNear(new ShinranshinPacket(lease.level.dimension().location(), lease.id, caster.getId(),
                lease.targetId, stage, remaining, lease.origin, lease.contact), lease.level, lease.origin.lerp(lease.contact, 0.5), 32);
    }

    private record Swap(WeakReference<LivingEntity> previous, WeakReference<LivingEntity> reference, @Nullable UUID assigned) {
        private Swap(@Nullable LivingEntity previous, @Nullable LivingEntity forced) {
            this(new WeakReference<>(previous), new WeakReference<>(forced), forced == null ? null : forced.getUUID());
        }
    }

    private static final class Lease {
        private final ServerLevel level;
        private final int id;
        private final WeakReference<LivingEntity> target;
        private final int targetId;
        private final long opens;
        private long expires;
        private boolean installed;
        private @Nullable Swap swap;
        private Vec3 origin;
        private Vec3 contact;

        private Lease(ServerLevel level, int id, LivingEntity target, Vec3 origin) {
            this.level = level;
            this.id = id;
            this.target = new WeakReference<>(target);
            this.targetId = target.getId();
            this.origin = origin;
            this.contact = JutsuAim.center(target);
            this.opens = level.getGameTime() + Shinranshin.FOCUS;
        }
    }
}
