package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ShinraWavePacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.ShinraWave;
import com.sekwah.narutomod.util.ShinraWave.Mode;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShinraWaveEvents {
    public static final float COST = 110f;
    private static final Map<ServerPlayer, Cast> WAVES = new WeakHashMap<>();

    private ShinraWaveEvents() {
    }

    public static boolean start(ServerPlayer owner, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (!owner.isAlive() || owner.isSpectator() || owner.noPhysics || !data.isNinjaModeEnabled()
                || data.getChakra() < COST || WAVES.containsKey(owner)) {
            return false;
        }
        data.useChakra(COST, 60);
        data.setCastPoseTicks(ShinraWave.CHARGE);
        var cast = new Cast(owner, new Receipt(data, success, data.getRankDamageMultiplier()));
        WAVES.put(owner, cast);
        send(owner, cast, Mode.CHARGE, cast.origin);
        tone(cast.level, cast.origin, NarutoSounds.SHINRA_PRESSURE.get(), 0.69f);
        return true;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        Cast cast = WAVES.get(owner);
        if (cast == null) {
            return;
        }
        if (!owner.isAlive() || owner.isSpectator() || owner.noPhysics || owner.level() != cast.level
                || !cast.receipt.data().isNinjaModeEnabled() || cast.center == null && owner.distanceToSqr(cast.origin) > 4 * 4) {
            cancel(owner, cast);
            return;
        }
        int age = (int) (cast.level.getGameTime() - cast.start);
        if (age < ShinraWave.CHARGE) {
            return;
        }
        if (cast.center == null) {
            release(owner, cast);
        } else {
            reach(owner, cast, age - ShinraWave.CHARGE);
        }
    }

    private static void release(ServerPlayer owner, Cast cast) {
        Vec3 at = center(owner);
        cast.center = at;
        cast.receipt.success().accept(owner, cast.receipt.data());
        send(owner, cast, Mode.PULSE, at);
        tone(cast.level, at, NarutoSounds.SHINRA_RELEASE.get(), 1.1f);
    }

    private static void reach(ServerPlayer owner, Cast cast, int age) {
        Vec3 at = cast.center;
        if (at == null) {
            return;
        }
        var front = new Front(at, ShinraWave.radius(age - 1), ShinraWave.radius(age));
        var area = new AABB(at, at).inflate(front.next());
        for (LivingEntity victim : cast.level.getEntitiesOfClass(LivingEntity.class, area,
                target -> JutsuAim.attackable(owner, target) && !cast.struck.contains(target.getUUID()))) {
            strike(owner, cast, front, victim);
        }
        if (age % 2 == 0) {
            ground(owner, cast, front, age);
        }
        if (age >= ShinraWave.EXPAND) {
            WAVES.remove(owner);
            tone(cast.level, at, NarutoSounds.SHINRA_WAKE.get(), 0.63f);
        }
    }

    private static void strike(ServerPlayer owner, Cast cast, Front front, LivingEntity target) {
        Vec3 nearest = WaterDragonFlight.nearestPoint(target.getBoundingBox(), front.center());
        double distance = nearest.distanceTo(front.center());
        if (!ShinraWave.touches(distance, front.previous(), front.next())
                || !cast.level.mayInteract(owner, target.blockPosition())
                || !clear(cast.level, owner, front.center(), JutsuAim.center(target))) {
            return;
        }
        cast.struck.add(target.getUUID());
        float raw = (float) (15.4 * cast.receipt.rank() * ShinraWave.falloff(distance));
        float damage = Math.min(raw, target instanceof Player ? 10f : 21f);
        if (damage <= 0 || !target.hurt(owner.damageSources().playerAttack(owner), damage)) {
            return;
        }
        Vec3 push = ShinraWave.impulse(front.center(), JutsuAim.center(target), cast.aim,
                target instanceof Player, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        target.setDeltaMovement(target.getDeltaMovement().scale(0.66).add(push));
        target.hurtMarked = true;
    }

    private static void ground(ServerPlayer owner, Cast cast, Front front, int age) {
        for (int i = 0; i < 8; i++) {
            double angle = (i + (age >> 1) * 0.38) * Math.PI / 4;
            double reach = Math.max(0, front.next() - 0.61);
            Vec3 flat = front.center().add(Math.cos(angle) * reach, 0, Math.sin(angle) * reach);
            if (!cast.level.hasChunkAt(BlockPos.containing(flat))) {
                continue;
            }
            var floor = cast.level.clip(new ClipContext(flat.add(0, 0.36, 0), flat.subtract(0, 2.8, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            Vec3 at = floor.getLocation().add(0, 0.038, 0);
            if (floor.getType() == HitResult.Type.BLOCK && ShinraWave.touches(at.distanceTo(front.center()), front.previous(), front.next())
                    && clear(cast.level, owner, front.center(), at)) {
                var state = cast.level.getBlockState(floor.getBlockPos());
                cast.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        at.x, at.y, at.z, 3, 0.25, 0.10, 0.25, 0.077);
            }
        }
    }

    private static boolean clear(ServerLevel level, Player caster, Vec3 from, Vec3 to) {
        return to.distanceToSqr(from) < 1.0e-8 || level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getType() == HitResult.Type.MISS;
    }

    private static Vec3 center(ServerPlayer owner) { return owner.position().add(0, 0.63 * owner.getBbHeight(), 0); }

    private static void send(ServerPlayer owner, Cast cast, Mode phase, Vec3 at) {
        PacketHandler.sendNear(new ShinraWavePacket(cast.level.dimension().location(), owner.getId(), phase, at), cast.level, at, 43);
    }

    private static void tone(ServerLevel level, Vec3 at, SoundEvent event, float volume) {
        level.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, 1.0f);
    }

    private static void cancel(ServerPlayer owner, Cast cast) {
        if (WAVES.remove(owner, cast) && cast.center == null) {
            cast.receipt.data().addChakra(COST);
            send(owner, cast, Mode.CANCEL, cast.origin);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            Cast cast = WAVES.get(owner);
            if (cast != null) {
                cancel(owner, cast);
            }
        }
    }

    private static final class Cast {
        private final ServerLevel level;
        private final long start;
        private final Vec3 origin;
        private final Vec3 aim;
        private final Receipt receipt;
        private final Set<UUID> struck = new HashSet<>();
        private @Nullable Vec3 center;

        private Cast(ServerPlayer owner, Receipt receipt) {
            this.level = owner.serverLevel();
            this.start = this.level.getGameTime();
            this.origin = ShinraWaveEvents.center(owner);
            this.aim = KamuiStep.facing(owner.getYRot());
            this.receipt = receipt;
        }
    }

    private record Front(Vec3 center, double previous, double next) {
    }

    private record Receipt(INinjaData data, BiConsumer<ServerPlayer, INinjaData> success, float rank) {
    }
}
