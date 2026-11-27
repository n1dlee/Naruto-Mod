package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.NagashiPacket;
import com.sekwah.narutomod.network.s2c.NagashiPacket.Phase;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.NagashiWave;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NagashiEvents {
    public static final int BRACE_TICKS = 3;
    private static final int WAVE_END = NagashiWave.EXPANSION_TICKS + NagashiWave.RESIDUAL_TICKS;
    private static final double LIFT = 0.20;
    private static final Map<LivingEntity, Cast> ACTIVE = new WeakHashMap<>();

    private NagashiEvents() {
    }

    public static void start(LivingEntity caster, Profile profile) {
        if (!(caster.level() instanceof ServerLevel level) || !caster.isAlive()
                || caster.isSpectator() || ACTIVE.containsKey(caster)) {
            return;
        }
        var cast = new Cast(profile, level.random.nextInt());
        ACTIVE.put(caster, cast);
        send(level, caster.getId(), cast, Phase.BRACE, caster.position(), List.of());
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), NarutoSounds.CHIDORI.get(),
                SoundSource.PLAYERS, 0.68f, 0.72f);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        for (LivingEntity caster : List.copyOf(ACTIVE.keySet())) {
            Cast cast = ACTIVE.get(caster);
            if (cast != null && caster.level() == level && !advance(level, caster, cast)) {
                ACTIVE.remove(caster, cast);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity caster) {
            ACTIVE.remove(caster);
        }
    }

    private static boolean advance(ServerLevel level, LivingEntity caster, Cast cast) {
        if (!caster.isAlive() || caster.isRemoved() || caster.isSpectator()) {
            return false;
        }
        int elapsed = ++cast.age - BRACE_TICKS;
        if (elapsed == 0) {
            cast.center = caster.position();
            release(level, caster, cast);
        } else if (elapsed > 0 && elapsed <= NagashiWave.EXPANSION_TICKS) {
            hitFront(level, caster, cast, elapsed);
        }
        return elapsed < WAVE_END;
    }

    private static void release(ServerLevel level, LivingEntity caster, Cast cast) {
        Vec3 start = cast.center.add(0, LIFT, 0);
        List<Float> reaches = new ArrayList<>(NagashiWave.SPOKES);
        for (int i = 0; i < NagashiWave.SPOKES; i++) {
            Vec3 to = start.add(NagashiWave.direction(i).scale(cast.profile.radius()));
            var hit = level.clip(new ClipContext(start, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            float reach = hit.getType() == HitResult.Type.BLOCK
                    ? Math.max(0.0f, (float) start.distanceTo(hit.getLocation()) - 0.025f)
                    : (float) cast.profile.radius();
            reaches.add(reach);
        }
        send(level, caster.getId(), cast, Phase.FRONT, cast.center, reaches);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 1.0f, 1.53f);
    }

    private static void hitFront(ServerLevel level, LivingEntity caster, Cast cast, int age) {
        double radius = NagashiWave.radius(age, cast.profile.radius());
        Vec3 center = cast.center;
        Vec3 start = center.add(0, LIFT, 0);
        AABB range = new AABB(center, center).inflate(radius, NagashiWave.FLOOR_TOLERANCE + 0.01, radius);
        var source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.CHIDORI, caster, caster);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, range, candidate -> hostile(caster, candidate))) {
            if (!NagashiWave.reaches(center, target.getY(), target.getBoundingBox(), radius)) {
                continue;
            }
            Vec3 near = WaterDragonFlight.nearestPoint(target.getBoundingBox(), start);
            if (!visible(level, caster, start, new Vec3(near.x, start.y, near.z))
                    || !visible(level, caster, start, near) || !cast.touched.add(target.getUUID())) {
                continue;
            }
            if (target.hurt(source, cast.profile.damage())) {
                boolean controlled = target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        cast.profile.stunTicks(), cast.profile.amplifier(), false, true));
                if (cast.profile.push()) {
                    Vec3 away = target.position().subtract(center);
                    target.knockback(1.2, -away.x, -away.z);
                }
                if (controlled) {
                    send(level, target.getId(), cast, Phase.HIT, target.getBoundingBox().getCenter(), List.of());
                }
            }
        }
    }

    private static boolean visible(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end) {
        return start.distanceToSqr(end) < 1.0e-8 || level.clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)
        ).getType() == HitResult.Type.MISS;
    }

    private static boolean hostile(LivingEntity caster, LivingEntity target) {
        if (!target.isAlive() || !target.isPickable() || target.isSpectator() || Faction.sameSide(caster, target)) {
            return false;
        }
        if (target instanceof Player victim) {
            return !victim.isCreative() && (!(caster instanceof Player player) || player.canHarmPlayer(victim));
        }
        return !caster.isAlliedTo(target);
    }

    private static void send(ServerLevel level, int actor, Cast cast, Phase phase, Vec3 pos, List<Float> reaches) {
        var packet = new NagashiPacket(level.dimension().location(), actor, phase, pos,
                (float) cast.profile.radius(), reaches, cast.seed);
        PacketHandler.sendNear(packet, level, pos, 48.0);
    }

    public record Profile(double radius, float damage, int stunTicks, int amplifier, boolean push) {
    }

    private static final class Cast {
        private final Profile profile;
        private final int seed;
        private final Set<UUID> touched = new HashSet<>();
        private int age;
        private Vec3 center = Vec3.ZERO;

        private Cast(Profile profile, int seed) {
            this.profile = profile;
            this.seed = seed;
        }
    }
}
