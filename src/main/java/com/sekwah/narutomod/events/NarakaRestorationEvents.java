package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.NarakaScenePacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.NarakaRestoration;
import com.sekwah.narutomod.util.NarakaRestoration.Stage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NarakaRestorationEvents {
    public static final float COST = 95;
    private static final Map<ServerPlayer, Memory> MEMORIES = new WeakHashMap<>();
    private static final Map<ServerPlayer, Scene> ACTIVE = new WeakHashMap<>();

    private NarakaRestorationEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void record(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ShadowCloneEntity clone) || clone.getType() != NarutoEntities.SHADOW_CLONE.get()
                || clone.isRestorationVessel() || clone.getBossVariant() >= 0 || !(clone.level() instanceof ServerLevel level)) {
            return;
        }
        var owner = clone.getOwnerUUID().map(level::getPlayerByUUID).orElse(null);
        if (owner instanceof ServerPlayer actor && actor.isAlive() && actor != event.getSource().getEntity()
                && actor.distanceToSqr(clone) <= 16 * 16) {
            MEMORIES.put(actor, new Memory(level, clone.position(), clone.getYRot(), clone.getUUID(),
                    level.getGameTime() + NarakaRestoration.MEMORY));
        }
    }

    public static boolean start(ServerPlayer owner, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (!valid(owner) || ACTIVE.containsKey(owner) || data.getChakra() < COST || !data.isNinjaModeEnabled()) {
            return false;
        }
        Subject subject = owner.isShiftKeyDown() ? rebuild(owner) : injured(owner);
        if (subject == null) {
            return false;
        }
        data.useChakra(COST, 60);
        data.setCastPoseTicks(subject.duration());
        ServerLevel level = owner.serverLevel();
        var scene = new Scene(level, level.getGameTime(), owner.position(), subject, new Payment(data, success));
        ACTIVE.put(owner, scene);
        send(owner, scene, subject instanceof Echo ? Stage.OPEN : Stage.HEALING);
        tone(level, subject.center(), subject instanceof Echo ? NarutoSounds.NARAKA_JAW.get() : NarutoSounds.NARAKA_RESTORE.get(), 0.71f);
        return true;
    }

    private static @Nullable Echo rebuild(ServerPlayer owner) {
        var memory = MEMORIES.get(owner);
        return memory != null && memoryValid(owner, memory) ? new Echo(memory) : null;
    }

    private static boolean memoryValid(ServerPlayer owner, Memory memory) {
        var level = owner.serverLevel();
        var original = level.getEntity(memory.original());
        if (!NarakaRestoration.usableEcho(level.getGameTime(), memory.expires(), level == memory.level(),
                original != null && original.isAlive())) {
            return false;
        }
        Vec3 pos = memory.center();
        BlockPos base = BlockPos.containing(pos);
        var volume = new AABB(-0.34, 0, -0.34, 0.34, 1.84, 0.34).move(pos);
        return owner.position().distanceToSqr(pos) <= NarakaRestoration.REACH * NarakaRestoration.REACH
                && level.hasChunksAt(BlockPos.containing(volume.minX, volume.minY, volume.minZ),
                BlockPos.containing(volume.maxX, volume.maxY, volume.maxZ))
                && level.mayInteract(owner, base) && level.getWorldBorder().isWithinBounds(base)
                && level.noCollision(owner, volume) && clear(owner, pos.add(0, 0.75, 0));
    }

    private static @Nullable Treatment injured(ServerPlayer owner) {
        Vec3 eye = owner.getEyePosition();
        Vec3 facing = owner.getLookAngle();
        Player target = owner.level().getEntitiesOfClass(Player.class, owner.getBoundingBox().inflate(NarakaRestoration.REACH),
                        ally -> ally != owner && patientValid(owner, ally)
                                && JutsuAim.aimed(eye, facing, JutsuAim.center(ally), NarakaRestoration.REACH, Math.cos(Math.toRadians(17))))
                .stream().min(Comparator.comparingDouble((Player ally) -> -JutsuAim.center(ally).subtract(eye).normalize().dot(facing))
                        .thenComparingDouble(owner::distanceToSqr).thenComparing(Player::getUUID)).orElse(null);
        if (target == null && patientValid(owner, owner)) {
            target = owner;
        }
        return target == null ? null : new Treatment(new WeakReference<>(target), target.position(), owner.getYRot());
    }

    private static boolean patientValid(ServerPlayer owner, Player target) {
        return valid(target) && owner.level() == target.level() && (target == owner || owner.isAlliedTo(target))
                && target.getHealth() < target.getMaxHealth() - 0.001f
                && owner.distanceToSqr(target) < NarakaRestoration.REACH * NarakaRestoration.REACH
                && (target == owner || clear(owner, JutsuAim.center(target)));
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        var scene = ACTIVE.get(owner);
        if (scene == null) {
            return;
        }
        if (!(valid(owner) && owner.level() == scene.level() && scene.pay().data().isNinjaModeEnabled())
                || owner.position().distanceToSqr(scene.origin()) > 3.2 * 3.2 || !subjectValid(owner, scene.subject())) {
            stop(owner, scene);
        } else if (scene.level().getGameTime() - scene.start() >= scene.subject().duration()) {
            if (complete(owner, scene)) {
                ACTIVE.remove(owner);
                scene.pay().success().accept(owner, scene.pay().data());
            } else {
                stop(owner, scene);
            }
        }
    }

    private static boolean subjectValid(ServerPlayer owner, Subject subject) {
        if (subject instanceof Echo echo) {
            return MEMORIES.get(owner) == echo.memory() && memoryValid(owner, echo.memory());
        }
        if (subject instanceof Treatment treatment) {
            Player patient = treatment.patient().get();
            return patient != null && patientValid(owner, patient);
        }
        return false;
    }

    private static boolean complete(ServerPlayer owner, Scene scene) {
        if (scene.subject() instanceof Echo echo) {
            var clone = NarutoEntities.SHADOW_CLONE.get().create(scene.level());
            if (clone == null) {
                return false;
            }
            clone.setPos(echo.center());
            clone.setYRot(echo.yaw());
            clone.setYBodyRot(echo.yaw());
            clone.setYHeadRot(echo.yaw());
            clone.setOwner(owner);
            clone.markRestoredVessel();
            if (!scene.level().addFreshEntity(clone)) {
                return false;
            }
            MEMORIES.remove(owner, echo.memory());
            send(owner, scene, Stage.REBUILT);
            tone(scene.level(), echo.center(), NarutoSounds.NARAKA_CLOSE.get(), 0.87f);
            return true;
        }
        if (scene.subject() instanceof Treatment treatment) {
            Player patient = treatment.patient().get();
            if (patient == null) {
                return false;
            }
            float old = patient.getHealth();
            patient.heal(NarakaRestoration.heal(patient.getMaxHealth() - old, scene.pay().data().getRankDamageMultiplier()));
            if (patient.getHealth() > old) {
                send(owner, scene, Stage.HEALED);
                tone(scene.level(), patient.position(), NarutoSounds.NARAKA_RESTORE.get(), 0.92f);
                return true;
            }
        }
        return false;
    }

    private static boolean clear(ServerPlayer owner, Vec3 at) {
        return owner.level().clip(new ClipContext(owner.getEyePosition(), at, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    private static boolean valid(Player actor) { return actor.isAlive() && !actor.isSpectator() && !actor.noPhysics; }

    private static void stop(ServerPlayer owner, Scene scene) {
        if (ACTIVE.remove(owner, scene)) {
            scene.pay().data().addChakra(COST);
            send(owner, scene, Stage.ABORT);
        }
    }

    private static void send(ServerPlayer owner, Scene scene, Stage stage) {
        var subject = scene.subject();
        var packet = new NarakaScenePacket(scene.level().dimension().location(), owner.getId(), stage, subject.center(),
                subject.yaw(), subject.entityId());
        PacketHandler.sendNear(packet, scene.level(), subject.center(), 36);
    }

    private static void tone(ServerLevel level, Vec3 at, SoundEvent sound, float volume) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, 1);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            var scene = ACTIVE.get(owner);
            if (scene != null) {
                stop(owner, scene);
            }
            MEMORIES.remove(owner);
        }
    }

    private sealed interface Subject permits Echo, Treatment {
        Vec3 center();
        float yaw();
        int duration();
        int entityId();
    }

    private record Echo(Memory memory) implements Subject {
        public Vec3 center() { return this.memory.center(); }
        public float yaw() { return this.memory.yaw(); }
        public int duration() { return NarakaRestoration.REBUILD_TICKS; }
        public int entityId() { return -1; }
    }

    private record Treatment(WeakReference<Player> patient, Vec3 anchor, float yaw) implements Subject {
        public Vec3 center() {
            Player p = this.patient.get();
            return p == null ? this.anchor : p.position();
        }
        public int duration() { return NarakaRestoration.HEAL_TICKS; }
        public int entityId() {
            Player p = this.patient.get();
            return p == null ? -1 : p.getId();
        }
    }

    private record Memory(ServerLevel level, Vec3 center, float yaw, UUID original, long expires) {
    }

    private record Scene(ServerLevel level, long start, Vec3 origin, Subject subject, Payment pay) {
    }

    private record Payment(INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
    }
}
