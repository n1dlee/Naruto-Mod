package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.MokutonTimberBlockEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.WoodCagePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WoodCageEvents {
    private static final double RANGE = 14;
    private static final Map<Player, Cage> ACTIVE = new WeakHashMap<>();
    private static final UUID PREVIEW = new UUID(0, 0);

    private WoodCageEvents() {
    }

    public static @Nullable Plan plan(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || owner.noPhysics || owner.isSpectator() || ACTIVE.containsKey(owner)) {
            return null;
        }
        var target = JutsuAim.select(owner, RANGE, 14, living -> living.onGround() && living.getBbWidth() < 2.6f);
        if (target == null) {
            return null;
        }
        BlockPos center = target.blockPosition();
        Vec3 delta = owner.position().subtract(Vec3.atBottomCenterOf(center));
        Direction gate = delta.horizontalDistanceSqr() > 0.01 ? Direction.getNearest(delta.x, 0, delta.z) : owner.getDirection().getOpposite();
        long count = WoodCagePlan.row(center, gate, 0).stream().filter(cell -> MokutonTerrain.free(level, owner, cell)
                && MokutonTimberBlockEntity.anchored(level, cell.support(), true, PREVIEW)).limit(4).count();
        return count >= 4 ? new Plan(target, center, gate) : null;
    }

    public static void start(Player owner, Plan plan) {
        if (owner.level() instanceof ServerLevel level && !ACTIVE.containsKey(owner)) {
            long now = level.getGameTime();
            var cage = new Cage(level, plan, now, UUID.randomUUID());
            ACTIVE.put(owner, cage);
            level.playSound(null, plan.center(), NarutoSounds.MOKUTON_WARN.get(), SoundSource.BLOCKS, 0.65f, 0.94f);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) {
            return;
        }
        Cage cage = ACTIVE.get(owner);
        if (cage == null) {
            return;
        }
        if (!owner.isAlive() || owner.noPhysics || owner.level() != cage.level || owner.isSpectator()) {
            ACTIVE.remove(owner);
            return;
        }
        int age = (int) (cage.level.getGameTime() - cage.start);
        if (age < WoodCagePlan.WINDUP) {
            if (age % 2 == 0) {
                warn(cage, owner);
            }
            return;
        }
        if (cage.row == 0 && !stillThere(owner, cage)) {
            ACTIVE.remove(owner);
            return;
        }
        if (age >= WoodCagePlan.WINDUP + cage.row * WoodCagePlan.ROW_INTERVAL) {
            int count = grow(owner, cage, cage.row++);
            if (count == 0 || cage.row > WoodCagePlan.ROOF) {
                ACTIVE.remove(owner);
            }
        }
    }

    private static boolean stillThere(Player owner, Cage cage) {
        LivingEntity target = cage.target.get();
        return target != null && JutsuAim.attackable(owner, target) && owner.hasLineOfSight(target)
                && target.position().distanceToSqr(Vec3.atBottomCenterOf(cage.center)) <= 1.87 * 1.87
                && owner.distanceToSqr(target) <= RANGE * RANGE;
    }

    private static void warn(Cage cage, Player owner) {
        for (var cell : WoodCagePlan.row(cage.center, cage.gate, 0)) {
            var pos = cell.pos();
            if (((pos.getX() + pos.getZ()) & 1) == 0 && MokutonTerrain.free(cage.level, owner, cell)
                    && MokutonTimberBlockEntity.anchored(cage.level, cell.support(), true, cage.id)) {
                cage.level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.08, pos.getZ() + 0.5,
                        1, 0.12, 0.025, 0.12, 0.0);
            }
        }
    }

    private static int grow(Player owner, Cage cage, int row) {
        int count = 0;
        for (var cell : WoodCagePlan.row(cage.center, cage.gate, row)) {
            if (MokutonTerrain.place(cage.level, owner, cell, cage.id, cage.expires)) {
                BlockPos pos = cell.pos();
                if ((count++ & 1) == 0) {
                    cage.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()),
                            pos.getX() + 0.5, pos.getY() + 0.28, pos.getZ() + 0.5, 3, 0.19, 0.14, 0.19, 0.053);
                }
            }
        }
        if (count > 0 && (row == 0 || row == WoodCagePlan.ROOF)) {
            cage.level.playSound(null, cage.center.above(row), NarutoSounds.MOKUTON_GROW.get(), SoundSource.BLOCKS,
                    0.83f, row == 0 ? 0.85f : 1.11f);
        }
        return count;
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player owner) {
            ACTIVE.remove(owner);
        }
    }

    public record Plan(LivingEntity target, BlockPos center, Direction gate) {
    }

    private static final class Cage {
        private final ServerLevel level;
        private final WeakReference<LivingEntity> target;
        private final BlockPos center;
        private final Direction gate;
        private final UUID id;
        private final long start;
        private final long expires;
        private int row;

        private Cage(ServerLevel level, Plan plan, long start, UUID id) {
            this.level = level;
            this.target = new WeakReference<>(plan.target());
            this.center = plan.center();
            this.gate = plan.gate();
            this.id = id;
            this.start = start;
            this.expires = start + WoodCagePlan.lifespan(plan.target() instanceof Player);
        }
    }
}
