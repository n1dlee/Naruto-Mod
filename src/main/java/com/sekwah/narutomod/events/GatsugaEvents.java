package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.NinkenEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.GatsugaPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.BoulderRoll;
import com.sekwah.narutomod.util.GatsugaMotion;
import com.sekwah.narutomod.util.GatsugaMotion.Stage;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GatsugaEvents {
    private static final Map<Player, Drill> ACTIVE = new WeakHashMap<>();
    private static int serial;

    private GatsugaEvents() {
    }

    public static boolean actionLocked(Player player) {
        return !player.level().isClientSide && ACTIVE.containsKey(player);
    }

    public static @Nullable NinkenEntity readyPartner(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || actionLocked(owner) || !suitable(owner)
                || !grounded(level, owner) || BoulderRollEvents.actionLocked(owner) || KaitenEvents.spinning(owner)
                || AdamantineChainEvents.sealed(owner)) {
            return null;
        }
        NinkenEntity dog = NinkenEvents.partner(owner);
        return dog != null && dog.readyForDrill() && grounded(level, dog) && owner.hasLineOfSight(dog)
                && dog.distanceToSqr(owner) <= GatsugaMotion.PARTNER_RANGE * GatsugaMotion.PARTNER_RANGE ? dog : null;
    }

    public static void start(Player player, NinkenEntity dog, float rank) {
        if (!(player.level() instanceof ServerLevel level) || readyPartner(player) != dog) {
            return;
        }
        Vec3 forward = KamuiStep.facing(player.getYRot());
        var drill = new Drill(level, ++serial, player, dog, forward, rank);
        ACTIVE.put(player, drill);
        dog.braceForDrill(GatsugaMotion.WINDUP + 2);
        send(drill, drill.human, Stage.BRACE, drill.human.point, drill.human.point.add(forward));
        send(drill, drill.wolf, Stage.BRACE, drill.wolf.point, drill.wolf.point.add(forward));
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        for (Player player : List.copyOf(ACTIVE.keySet())) {
            Drill drill = ACTIVE.get(player);
            if (drill != null && drill.level == level) {
                advance(player, drill);
            }
        }
    }

    private static void advance(Player player, Drill drill) {
        NinkenEntity dog = drill.dog.get();
        if (!suitable(player) || player.level() != drill.level || AdamantineChainEvents.sealed(player)) {
            finish(player, drill);
            return;
        }
        boolean partner = dog != null && suitable(dog) && dog.isOwnedBy(player) && dog.level() == drill.level && !dog.isOrderedToSit();
        if (drill.step == 0 && (!partner || !grounded(drill.level, dog) || !grounded(drill.level, player)
                || drill.playerStart.distanceToSqr(player.position()) > 1.55 * 1.55
                || dog.distanceToSqr(player) > GatsugaMotion.PARTNER_RANGE * GatsugaMotion.PARTNER_RANGE
                || !player.hasLineOfSight(dog))) {
            finish(player, drill);
            return;
        }
        if (drill.level.getGameTime() < drill.launch) {
            return;
        }
        if (drill.step == 0) {
            prepare(player, dog, drill);
        }
        float progress = ++drill.step / (float) GatsugaMotion.STEPS;
        if (drill.human.active) {
            segment(player, player, drill, drill.human, progress);
        }
        if (drill.wolf.active) {
            if (partner) {
                dog.braceForDrill(2);
                segment(player, dog, drill, drill.wolf, progress);
            } else {
                stop(drill, drill.wolf, Stage.STOP);
            }
        }
        if (drill.step >= GatsugaMotion.STEPS || !drill.human.active && !drill.wolf.active) {
            finish(player, drill);
        }
    }

    private static void prepare(Player player, NinkenEntity dog, Drill drill) {
        Vec3 left = new Vec3(drill.axis.z, 0, -drill.axis.x);
        if (dog.position().subtract(player.position()).dot(left) > 0) {
            left = left.reverse();
        }
        Vec3 apex = player.position().add(drill.axis.scale(9));
        drill.human.path = new GatsugaMotion.Lane(player.position(), apex.add(left.scale(0.42)), left, false);
        drill.wolf.path = new GatsugaMotion.Lane(dog.position(), apex.subtract(left.scale(0.42)), left, true);
        drill.level.playSound(null, player.blockPosition(), NarutoSounds.GATSUGA_RELEASE.get(), SoundSource.PLAYERS, 0.82f, 1.08f);
        drill.level.playSound(null, dog.blockPosition(), NarutoSounds.GATSUGA_RELEASE.get(), SoundSource.PLAYERS, 0.76f, 0.87f);
    }

    private static void segment(Player owner, LivingEntity body, Drill drill, Track lane, float progress) {
        Vec3 feet = body.position();
        if (feet.distanceToSqr(lane.feet) > 1.55 * 1.55 || !grounded(drill.level, body)) {
            stop(drill, lane, Stage.STOP);
            return;
        }
        Vec3 want = GatsugaMotion.step(feet, GatsugaMotion.path(lane.path, progress));
        Vec3 from = center(body, lane.wolf);
        if (!drill.level.hasChunkAt(BlockPos.containing(feet.add(want)))) {
            stop(drill, lane, Stage.STOP);
            return;
        }
        body.move(MoverType.SELF, want);
        Vec3 actual = body.position().subtract(feet);
        lane.point = center(body, lane.wolf);
        lane.feet = body.position();
        if (actual.horizontalDistanceSqr() >= 0.0004) {
            lane.axis = ProjectileAxis.direction(actual, drill.axis);
            sync(body);
            send(drill, lane, Stage.TRAVEL, from, lane.point);
            strike(owner, drill, lane, from);
        }
        if (GatsugaMotion.obstructed(want, actual)) {
            stop(drill, lane, Stage.BLOCK);
            dust(drill, body);
            drill.level.playSound(null, body.blockPosition(), NarutoSounds.GATSUGA_STOP.get(), SoundSource.PLAYERS, 0.63f, 1);
        }
    }

    private static void strike(Player caster, Drill drill, Track lane, Vec3 from) {
        double width = lane.wolf ? 0.50 : 0.63;
        AABB scan = new AABB(from, lane.point).inflate(width);
        List<LivingEntity> targets = drill.level.getEntitiesOfClass(LivingEntity.class, scan,
                target -> JutsuAim.attackable(caster, target));
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(from)));
        for (LivingEntity target : targets) {
            Vec3 point = BoulderRoll.contact(from, lane.point, target.getBoundingBox(), width);
            if (point != null && visible(drill.level, caster, lane.point, point) && drill.hit.mark(target.getUUID())) {
                float power = (lane.wolf ? 6.5f : 8.5f) * drill.rank;
                if (target.hurt(caster.damageSources().playerAttack(caster),
                        Math.min(power, target instanceof Player ? 6.5f : 15f))) {
                    target.knockback(0.86, -lane.axis.x, -lane.axis.z);
                    target.hurtMarked = true;
                    send(drill, lane, Stage.CONTACT, lane.point, point);
                }
            }
        }
    }

    private static boolean visible(ServerLevel level, Player caster, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster))
                .getType() == HitResult.Type.MISS;
    }

    private static void sync(LivingEntity actor) {
        actor.setDeltaMovement(0, actor.getDeltaMovement().y, 0);
        if (actor instanceof ServerPlayer player) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        } else {
            actor.hurtMarked = true;
        }
    }

    private static boolean suitable(LivingEntity actor) {
        return actor.isAlive() && !actor.isRemoved() && !actor.isSpectator() && !actor.noPhysics && !actor.isPassenger()
                && !actor.isInWaterOrBubble() && !actor.isFallFlying();
    }

    private static boolean grounded(ServerLevel level, LivingEntity body) {
        return body.onGround() || level.getBlockCollisions(body,
                body.getBoundingBox().deflate(0.01).move(0, -0.14, 0)).iterator().hasNext();
    }

    private static Vec3 center(LivingEntity entity, boolean wolf) {
        return entity.position().add(0, entity.getBbHeight() * (wolf ? 0.54 : 0.48), 0);
    }

    private static void dust(Drill drill, LivingEntity body) {
        var block = drill.level.getBlockState(body.getOnPos());
        if (!block.isAir()) {
            drill.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block), body.getX(), body.getY() + 0.11,
                    body.getZ(), 7, 0.29, 0.08, 0.29, 0.052);
        }
    }

    private static void stop(Drill drill, Track lane, Stage stage) {
        if (lane.active) {
            lane.active = false;
            send(drill, lane, stage, lane.point, lane.point.add(lane.axis));
        }
    }

    private static void finish(Player caster, Drill drill) {
        stop(drill, drill.human, Stage.STOP);
        stop(drill, drill.wolf, Stage.STOP);
        ACTIVE.remove(caster);
    }

    private static void send(Drill drill, Track lane, Stage stage, Vec3 from, Vec3 to) {
        PacketHandler.sendNear(new GatsugaPacket(drill.level.dimension().location(), drill.id, lane.id, stage, lane.wolf,
                from, to), drill.level, from.lerp(to, 0.5), 34);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            Drill drill = ACTIVE.get(player);
            if (drill != null) {
                finish(player, drill);
            }
        }
    }

    @SubscribeEvent
    public static void blockMelee(AttackEntityEvent event) {
        if (actionLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static final class Track {
        private final int id;
        private final boolean wolf;
        private boolean active = true;
        private GatsugaMotion.Lane path;
        private Vec3 axis;
        private Vec3 point;
        private Vec3 feet;

        private Track(LivingEntity entity, Vec3 axis, boolean wolf) {
            this.id = entity.getId();
            this.wolf = wolf;
            this.axis = axis;
            this.point = center(entity, wolf);
            this.feet = entity.position();
        }
    }

    private static final class Drill {
        private final ServerLevel level;
        private final int id;
        private final long launch;
        private final Vec3 playerStart;
        private final Vec3 axis;
        private final float rank;
        private final WeakReference<NinkenEntity> dog;
        private final Track human;
        private final Track wolf;
        private final GatsugaMotion.HitLedger hit = new GatsugaMotion.HitLedger();
        private int step;

        private Drill(ServerLevel level, int id, Player player, NinkenEntity dog, Vec3 axis, float rank) {
            this.level = level;
            this.id = id;
            this.axis = axis;
            this.rank = rank;
            this.playerStart = player.position();
            this.launch = level.getGameTime() + GatsugaMotion.WINDUP;
            this.dog = new WeakReference<>(dog);
            this.human = new Track(player, axis, false);
            this.wolf = new Track(dog, axis, true);
        }
    }
}
