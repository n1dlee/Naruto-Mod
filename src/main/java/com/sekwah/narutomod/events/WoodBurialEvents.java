package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.WoodBurialWavePacket;
import com.sekwah.narutomod.network.s2c.WoodBurialWavePacket.Mode;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.WoodBurialPath;
import com.sekwah.narutomod.util.WoodCagePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
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

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WoodBurialEvents {
    private static final Map<Player, Wave> ACTIVE = new WeakHashMap<>();

    private WoodBurialEvents() {
    }

    public static boolean canStart(Player owner) {
        return owner.level() instanceof ServerLevel server && !ACTIVE.containsKey(owner) && owner.isAlive() && !owner.noPhysics
                && !owner.isSpectator() && owner.onGround()
                && trace(server, owner, owner.position(), KamuiStep.facing(owner.getYRot())) != null;
    }

    public static void start(Player owner, float rank) {
        if (owner.level() instanceof ServerLevel level && canStart(owner)) {
            var wave = new Wave(level, owner.position(), KamuiStep.facing(owner.getYRot()), rank);
            ACTIVE.put(owner, wave);
            level.playSound(null, owner.blockPosition(), NarutoSounds.MOKUTON_WARN.get(), SoundSource.BLOCKS, 0.66f, 0.83f);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) {
            return;
        }
        Wave wave = ACTIVE.get(owner);
        if (wave == null) {
            return;
        }
        if (!owner.isAlive() || owner.noPhysics || owner.isSpectator() || owner.level() != wave.level) {
            ACTIVE.remove(owner);
            return;
        }
        int age = (int) (wave.level.getGameTime() - wave.start);
        int delay = WoodBurialPath.beat(wave.step) - age;
        if (delay == (WoodBurialPath.finalBeat(wave.step) ? 5 : 3)) {
            var next = trace(wave.level, owner, wave.previous, wave.axis);
            if (next == null) {
                ACTIVE.remove(owner);
            } else {
                emit(wave, Mode.TRACE, next);
            }
        } else if (delay <= 0) {
            var next = trace(wave.level, owner, wave.previous, wave.axis);
            if (next == null || !erupt(owner, wave, next) || ++wave.step >= WoodBurialPath.STEPS) {
                ACTIVE.remove(owner);
            } else {
                wave.previous = next.end();
            }
        }
    }

    @Nullable
    private static WoodBurialPath.Route trace(ServerLevel level, Player owner, Vec3 from, Vec3 axis) {
        return WoodBurialPath.route(from, axis, (requested, previous) -> supported(level, owner, requested, previous));
    }

    private static @Nullable Vec3 supported(ServerLevel level, Player owner, Vec3 requested, Vec3 previous) {
        BlockPos column = BlockPos.containing(requested);
        if (!level.hasChunkAt(column)) {
            return null;
        }
        for (int rise : new int[]{0, 1, -1}) {
            BlockPos pos = column.above(rise);
            BlockPos below = pos.below();
            if (!room(level, owner, pos) || !level.getBlockState(below).getFluidState().isEmpty()
                    || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP, SupportType.CENTER)) {
                continue;
            }
            Vec3 next = new Vec3(requested.x, pos.getY(), requested.z);
            double y = Math.max(previous.y, next.y) + 0.19;
            Vec3 highPrevious = new Vec3(previous.x, y, previous.z);
            Vec3 highNext = new Vec3(next.x, y, next.z);
            if (open(level, owner, previous.add(0, 0.19, 0), highPrevious)
                    && open(level, owner, highPrevious, highNext) && open(level, owner, highNext, next.add(0, 0.19, 0))) {
                return next;
            }
        }
        return null;
    }

    private static boolean room(ServerLevel level, Player owner, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(owner, pos) && level.isEmptyBlock(pos);
    }

    private static boolean erupt(Player owner, Wave wave, WoodBurialPath.Route route) {
        Vec3 center = route.end();
        Set<BlockPos> bases = new HashSet<>();
        boolean heavy = WoodBurialPath.finalBeat(wave.step);
        Vec3 right = new Vec3(wave.axis.z, 0, -wave.axis.x);
        var base = BlockPos.containing(center);
        Vec3 middle = Vec3.atBottomCenterOf(base);
        for (double offset : heavy ? new double[]{-1.50, -0.79, 0.79, 1.50} : new double[]{-0.82, 0.82}) {
            BlockPos branch = BlockPos.containing(middle.add(right.scale(offset)));
            if (!branch.equals(base) && bases.add(branch)) {
                if (stem(owner, wave, branch, heavy)) {
                    strike(owner, wave, Vec3.atBottomCenterOf(branch), center, heavy);
                } else {
                    bases.remove(branch);
                }
            }
        }
        if (bases.isEmpty()) {
            return false;
        }
        emit(wave, Mode.BURST, route);
        wave.level.playSound(null, base, (heavy ? NarutoSounds.MOKUTON_BURIAL.get() : NarutoSounds.MOKUTON_GROW.get()),
                SoundSource.BLOCKS, heavy ? 1.04f : 0.54f, heavy ? 0.87f : 1.09f);
        return true;
    }

    private static boolean stem(Player owner, Wave wave, BlockPos base, boolean heavy) {
        var profile = Math.abs(wave.axis.x) > Math.abs(wave.axis.z) ? WoodCagePlan.Profile.ROOT_X : WoodCagePlan.Profile.ROOT_Z;
        int rows = heavy ? 3 : 2;
        for (int row = 0; row < rows; row++) {
            BlockPos at = base.above(row);
            var cell = new WoodCagePlan.Cell(at, row == 0 ? profile : WoodCagePlan.Profile.POST, at.below(), row == 0);
            if (!MokutonTerrain.place(wave.level, owner, cell, wave.id, wave.start + 105)) {
                return row > 0;
            }
        }
        wave.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()),
                base.getX() + 0.5, base.getY() + 0.3, base.getZ() + 0.5, 4, 0.19, 0.2, 0.19, 0.04);
        return true;
    }

    private static void strike(Player owner, Wave wave, Vec3 root, Vec3 center, boolean heavy) {
        Vec3 start = root.lerp(center, 0.37).add(0, 0.29, 0);
        Vec3 tip = center.add(0, heavy ? 1.42 : 0.97, 0);
        for (LivingEntity target : wave.level.getEntitiesOfClass(LivingEntity.class, new AABB(start, tip).inflate(0.54),
                actor -> JutsuAim.attackable(owner, actor) && !wave.struck.contains(actor.getUUID()))) {
            if (!wave.level.mayInteract(owner, target.blockPosition()) || !WoodBurialPath.swept(start, tip, target.getBoundingBox(), heavy)
                    || !open(wave.level, owner, center.add(0, 0.32, 0), JutsuAim.center(target))) {
                continue;
            }
            wave.struck.add(target.getUUID());
            float amount = Math.min((heavy ? 7.4f : 5.1f) * wave.rank, target instanceof Player ? 6f : 12f);
            if (target.hurt(owner.damageSources().playerAttack(owner), amount)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, heavy ? 33 : 23, 0, false, true));
            }
        }
    }

    private static boolean open(ServerLevel level, Player owner, Vec3 from, Vec3 to) {
        return from.distanceToSqr(to) < 1.0e-8 || level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    private static void emit(Wave wave, Mode mode, WoodBurialPath.Route route) {
        var packet = new WoodBurialWavePacket(wave.level.dimension().location(), wave.id.hashCode() + wave.step,
                mode, WoodBurialPath.finalBeat(wave.step), route.samples());
        PacketHandler.sendNear(packet, wave.level, wave.previous.lerp(route.end(), 0.5), 34);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player owner) {
            ACTIVE.remove(owner);
        }
    }

    private static final class Wave {
        private final ServerLevel level;
        private final long start;
        private final UUID id = UUID.randomUUID();
        private final Vec3 axis;
        private final float rank;
        private final Set<UUID> struck = new HashSet<>();
        private Vec3 previous;
        private int step;

        private Wave(ServerLevel level, Vec3 from, Vec3 axis, float rank) {
            this.level = level;
            this.start = level.getGameTime();
            this.previous = from;
            this.axis = axis;
            this.rank = rank;
        }
    }
}
