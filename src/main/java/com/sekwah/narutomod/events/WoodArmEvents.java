package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.WoodArmPacket;
import com.sekwah.narutomod.network.s2c.WoodArmPacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WaterDragonFlight.Contact;
import com.sekwah.narutomod.util.WoodArmMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
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
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WoodArmEvents {
    private static final Map<Player, Cast> ACTIVE = new WeakHashMap<>();
    private static int serial;

    private WoodArmEvents() {
    }

    public static boolean canStart(Player owner) {
        return owner.level() instanceof ServerLevel && usable(owner) && !ACTIVE.containsKey(owner);
    }

    public static void start(Player owner, float rank) {
        if (owner.level() instanceof ServerLevel level && canStart(owner)) {
            var cast = new Cast(level, ++serial, rank, owner);
            ACTIVE.put(owner, cast);
            send(cast, Stage.BRACE, cast.tip, cast.tip);
            sound(cast, NarutoSounds.MOKUTON_WARN.get(), 0.47f);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) {
            return;
        }
        Cast cast = ACTIVE.get(owner);
        if (cast == null) {
            return;
        }
        if (!usable(owner) || owner.level() != cast.level || shoulder(owner).distanceToSqr(cast.origin) > 3.1 * 3.1) {
            end(owner, cast, Stage.BREAK);
            return;
        }
        cast.base = shoulder(owner);
        int age = (int) (cast.level.getGameTime() - cast.start);
        if (cast.gripped != null) {
            grip(owner, cast, age);
        } else if (age >= WoodArmMotion.WINDUP) {
            extend(owner, cast, age - WoodArmMotion.WINDUP);
        }
    }

    private static void extend(Player owner, Cast cast, int age) {
        if (age == 0) {
            cast.tip = cast.base;
            sound(cast, NarutoSounds.MOKUTON_GROW.get(), 0.68f);
        }
        Vec3 from = cast.tip;
        double step = WoodArmMotion.step(age, WoodArmMotion.RANGE - cast.distance);
        Vec3 to = from.add(cast.axis.scale(step));
        AABB path = new AABB(from, to).inflate(WoodArmMotion.RADIUS + 0.001);
        if (!available(cast.level, path) || !line(owner, cast.base, from)) {
            end(owner, cast, Stage.BREAK);
            return;
        }
        Contact block = blockContact(owner, cast, path, from, to);
        if (firstTarget(owner, cast, path, from, to, block)) {
            return;
        }
        if (block != null) {
            cast.tip = block.center();
            end(owner, cast, Stage.BREAK);
        } else if (!cast.level.mayInteract(owner, BlockPos.containing(to))) {
            end(owner, cast, Stage.BREAK);
        } else {
            cast.tip = to;
            cast.distance += step;
            send(cast, Stage.EXTEND, from, to);
            if (cast.distance >= WoodArmMotion.RANGE - 1.0e-6) {
                end(owner, cast, Stage.RETRACT);
            }
        }
    }

    private static @Nullable Contact blockContact(Player owner, Cast cast, AABB path, Vec3 from, Vec3 to) {
        Contact first = null;
        for (var shape : cast.level.getBlockCollisions(owner, path)) {
            for (AABB box : shape.toAabbs()) {
                Contact contact = WaterDragonFlight.sweep(from, to, box, WoodArmMotion.RADIUS);
                if (contact != null && contact.precedes(first)) {
                    first = contact;
                }
            }
        }
        return first;
    }

    private static boolean firstTarget(Player owner, Cast cast, AABB path, Vec3 from, Vec3 to, @Nullable Contact block) {
        Contact first = block;
        LivingEntity hit = null;
        for (var candidate : cast.level.getEntitiesOfClass(LivingEntity.class, path, actor -> JutsuAim.attackable(owner, actor))) {
            Contact contact = WaterDragonFlight.sweep(from, to, candidate.getBoundingBox(), WoodArmMotion.RADIUS);
            if (WoodArmMotion.hits(contact, first) && cast.level.mayInteract(owner, candidate.blockPosition())
                    && line(owner, cast.base, contact.point())) {
                first = contact;
                hit = candidate;
            }
        }
        if (hit == null || first == null) {
            return false;
        }
        cast.tip = first.point();
        float damage = Math.min(6f * cast.rank, hit instanceof Player ? 5.5f : 12f);
        if (hit.hurt(owner.damageSources().playerAttack(owner), damage) && hit.isAlive()) {
            cast.gripped = new WeakReference<>(hit);
            cast.release = cast.level.getGameTime() + 4;
            send(cast, Stage.GRIP, from, cast.tip);
            sound(cast, NarutoSounds.MOKUTON_GRAB.get(), 0.71f);
        } else {
            end(owner, cast, Stage.BREAK);
        }
        return true;
    }

    private static void grip(Player owner, Cast cast, int age) {
        LivingEntity target = cast.gripped.get();
        if (target == null || !JutsuAim.attackable(owner, target) || age > 20
                || cast.level.getGameTime() >= cast.release || !cast.level.mayInteract(owner, target.blockPosition())) {
            end(owner, cast, Stage.RETRACT);
            return;
        }
        Vec3 center = JutsuAim.center(target);
        Vec3 tip = WaterDragonFlight.nearestPoint(target.getBoundingBox(), cast.base.lerp(center, 0.82));
        Vec3 pull = WoodArmMotion.pull(center, cast.base, target instanceof Player);
        AABB swept = target.getBoundingBox().expandTowards(pull);
        if (center.distanceToSqr(cast.base) > (WoodArmMotion.RANGE + 1) * (WoodArmMotion.RANGE + 1)
                || !available(cast.level, swept) || !line(owner, cast.base, tip) || !cast.level.noCollision(target, swept)) {
            end(owner, cast, Stage.BREAK);
            return;
        }
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(pull.x + motion.x * 0.28, motion.y, pull.z + motion.z * 0.28);
        target.hurtMarked = true;
        send(cast, Stage.GRIP, cast.tip, tip);
        cast.tip = tip;
    }

    private static boolean line(Player owner, Vec3 from, Vec3 to) {
        return from.distanceToSqr(to) < 1.0e-8 || owner.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    private static boolean available(ServerLevel level, AABB box) {
        var border = level.getWorldBorder();
        return KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                && level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
                && level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.maxZ))
                && level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.minZ))
                && level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.maxZ));
    }

    private static boolean usable(Player owner) {
        return owner.isAlive() && !owner.isSpectator() && !owner.noPhysics;
    }

    private static Vec3 shoulder(Player owner) {
        Vec3 forward = KamuiStep.facing(owner.getYRot());
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return owner.getEyePosition().add(0, -0.28, 0)
                .add(right.scale(owner.getMainArm() == HumanoidArm.RIGHT ? 0.31 : -0.31)).add(forward.scale(0.19));
    }

    private static void end(Player owner, Cast cast, Stage stage) {
        if (ACTIVE.remove(owner, cast)) {
            send(cast, stage, cast.tip, cast.tip);
            if (stage == Stage.BREAK) {
                sound(cast, NarutoSounds.MOKUTON_GRAB.get(), 0.35f);
            }
        }
    }

    private static void send(Cast cast, Stage stage, Vec3 from, Vec3 tip) {
        PacketHandler.sendNear(new WoodArmPacket(cast.level.dimension().location(), cast.serial, stage, cast.base, from, tip),
                cast.level, cast.base.lerp(tip, 0.5), 36);
    }

    private static void sound(Cast cast, SoundEvent sound, float volume) {
        cast.level.playSound(null, cast.tip.x, cast.tip.y, cast.tip.z, sound, SoundSource.PLAYERS, volume, 1);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player owner) {
            ACTIVE.remove(owner);
        }
    }

    private static final class Cast {
        private final ServerLevel level;
        private final int serial;
        private final long start;
        private final Vec3 origin;
        private final Vec3 axis;
        private final float rank;
        private Vec3 base;
        private Vec3 tip;
        private double distance;
        private long release;
        @Nullable private WeakReference<LivingEntity> gripped;

        private Cast(ServerLevel level, int serial, float rank, Player owner) {
            this.level = level;
            this.serial = serial;
            this.start = level.getGameTime();
            this.origin = shoulder(owner);
            this.axis = owner.getLookAngle();
            this.rank = rank;
            this.base = this.origin;
            this.tip = this.origin.add(this.axis.scale(0.42));
        }
    }
}
