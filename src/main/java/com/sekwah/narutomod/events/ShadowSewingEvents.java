package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket.Cue;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.ShadowNeedle;
import com.sekwah.narutomod.util.ShadowSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowSewingEvents {
    private static final Map<LivingEntity, Volley> PENDING = new WeakHashMap<>();
    private static int serial;

    private ShadowSewingEvents() {
    }

    public static boolean canStart(LivingEntity caster) {
        return caster.level() instanceof ServerLevel level && caster.isAlive() && caster.onGround() && !caster.noPhysics
                && !caster.isSpectator() && !PENDING.containsKey(caster) && level.getMaxLocalRawBrightness(caster.blockPosition()) >= 4;
    }

    public static void start(LivingEntity caster, double range, float damage, @Nullable LivingEntity preferred) {
        if (!canStart(caster) || !(caster.level() instanceof ServerLevel level)) {
            return;
        }
        double reach = Math.min(12, Math.max(3, range));
        Vec3 origin = caster.position();
        List<Branch> routes = new ArrayList<>();
        for (LivingEntity candidate : candidates(level, caster, reach, preferred)) {
            List<Vec3> route = ShadowTetherEvents.surfacePath(level, caster, origin, candidate.position());
            if (route.size() < 2) {
                continue;
            }
            boolean reaches = ShadowSurface.reaches(route, candidate.position());
            routes.add(new Branch(--serial, new WeakReference<>(reaches ? candidate : null), route));
            if (routes.size() == ShadowNeedle.BRANCHES) {
                break;
            }
        }
        if (routes.isEmpty() && preferred == null) {
            Vec3 aim = com.sekwah.narutomod.util.KamuiStep.facing(caster.getYRot());
            List<Vec3> fallback = ShadowTetherEvents.surfacePath(level, caster, origin, origin.add(aim.scale(reach)));
            if (fallback.size() >= 2) {
                routes.add(new Branch(--serial, new WeakReference<>(null), fallback));
            }
        }
        if (!routes.isEmpty()) {
            PENDING.put(caster, new Volley(level, origin, level.getGameTime() + ShadowNeedle.RISE_AT, damage, List.copyOf(routes)));
            for (Branch branch : routes) {
                visual(level, caster, branch.id(), Cue.SEW_TRACE, branch.path());
            }
            level.playSound(null, caster.blockPosition(), NarutoSounds.SHADOW_WEAVE.get(), SoundSource.PLAYERS, 0.53f, 1.08f);
        }
    }

    private static List<LivingEntity> candidates(ServerLevel level, LivingEntity caster, double range, @Nullable LivingEntity preferred) {
        if (preferred != null) {
            return eligible(caster, preferred) && caster.distanceToSqr(preferred) <= range * range ? List.of(preferred) : List.of();
        }
        return level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range, 3.0, range),
                target -> eligible(caster, target) && caster.distanceToSqr(target) <= range * range).stream()
                .sorted(Comparator.comparingDouble(target -> caster.distanceToSqr(target))).limit(9).toList();
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity actor = event.getEntity();
        if (!(actor.level() instanceof ServerLevel level)) {
            return;
        }
        Volley volley = PENDING.get(actor);
        if (volley == null) {
            return;
        }
        if (!actor.isAlive() || actor.isSpectator() || actor.noPhysics || level != volley.level()
                || actor.position().distanceToSqr(volley.origin()) > 2.25) {
            abort(actor, volley);
        } else if (level.getGameTime() >= volley.release()) {
            PENDING.remove(actor);
            for (Branch branch : volley.branches()) {
                rise(actor, volley, branch);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity caster) {
            Volley volley = PENDING.get(caster);
            if (volley != null) {
                abort(caster, volley);
            }
        }
    }

    private static void abort(LivingEntity caster, Volley volley) {
        PENDING.remove(caster);
        for (Branch branch : volley.branches()) {
            visual(volley.level(), caster, branch.id(), Cue.SEW_MISS, branch.path());
        }
    }

    private static void rise(LivingEntity caster, Volley volley, Branch branch) {
        ServerLevel level = volley.level();
        Vec3 feet = branch.path().get(branch.path().size() - 1);
        List<Vec3> route = ShadowTetherEvents.surfacePath(level, caster, volley.origin(), feet);
        LivingEntity target = branch.target().get();
        boolean hit = false;
        if (target != null && eligible(caster, target) && ShadowSurface.reaches(route, feet)
                && ShadowNeedle.reaches(feet, target.position(), target.getBbWidth()) && openLift(level, caster, feet, target)) {
            var source = caster instanceof Player player ? caster.damageSources().playerAttack(player)
                    : caster.damageSources().indirectMagic(caster, caster);
            hit = target.hurt(source, volley.damage());
            if (hit) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, target instanceof Player ? 16 : 30,
                        2, false, false, true), caster);
                level.playSound(null, BlockPos.containing(feet), NarutoSounds.SHADOW_NEEDLE.get(), SoundSource.PLAYERS, 0.68f, 1f);
            }
        }
        List<Vec3> drawing = route.size() >= 2 ? route : branch.path().subList(0, 2);
        visual(level, caster, branch.id(), hit ? Cue.SEW_HIT : Cue.SEW_MISS, drawing);
    }

    private static boolean openLift(ServerLevel level, LivingEntity caster, Vec3 ground, LivingEntity victim) {
        Vec3 top = victim.position().add(0, Math.min(ShadowNeedle.STRIKE_HEIGHT, victim.getBbHeight() * 0.56), 0);
        return level.clip(new ClipContext(ground.add(0, 0.07, 0), top, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getType() == HitResult.Type.MISS;
    }

    private static boolean eligible(LivingEntity caster, LivingEntity target) {
        return caster != target && target.isAlive() && !target.isSpectator() && !target.noPhysics && target.level() == caster.level()
                && !caster.isAlliedTo(target) && !Faction.sameSide(caster, target)
                && !(target instanceof TamableAnimal pet && pet.isTame()) && (!(target instanceof Player player)
                || !player.isCreative() && (!(caster instanceof Player source) || source.canHarmPlayer(player)));
    }

    private static void visual(ServerLevel level, LivingEntity caster, int id, Cue cue, List<Vec3> points) {
        PacketHandler.sendNear(new ShadowTetherPacket(level.dimension().location(), caster.getId(), id, cue, points),
                level, points.get(0), 40);
    }

    private record Branch(int id, WeakReference<LivingEntity> target, List<Vec3> path) {
    }

    private record Volley(ServerLevel level, Vec3 origin, long release, float damage, List<Branch> branches) {
    }
}
