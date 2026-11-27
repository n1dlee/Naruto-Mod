package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.WoodGolemEntity;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.WoodGolemMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WoodGolemSummons {
    private static final Map<Player, WeakReference<WoodGolemEntity>> ACTIVE = new WeakHashMap<>();
    private static final int LEASE_TICKS = 5;

    private WoodGolemSummons() {
    }

    public static boolean summon(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()
                || owner.noPhysics || current(owner) != null) {
            return false;
        }
        var golem = NarutoEntities.WOOD_GOLEM.get().create(level);
        if (golem == null || !locate(level, owner, golem)) {
            return false;
        }
        golem.setOwner(owner);
        golem.setYRot(owner.getYRot());
        golem.yBodyRot = owner.getYRot();
        golem.keepStanding(level.getGameTime() + LEASE_TICKS);
        if (!level.addFreshEntity(golem)) {
            return false;
        }
        ACTIVE.put(owner, new WeakReference<>(golem));
        return true;
    }

    public static boolean refresh(Player owner) {
        WoodGolemEntity golem = current(owner);
        if (golem == null || !WoodGolemMotion.validLease(!golem.isRemoved() && !golem.dismantling(), golem.level() == owner.level(),
                owner.isAlive() && !owner.isSpectator() && golem.isAlive(), owner.level().getGameTime(), golem.lease())
                || owner.distanceToSqr(golem) > 48 * 48) {
            return false;
        }
        golem.keepStanding(owner.level().getGameTime() + LEASE_TICKS);
        return true;
    }

    public static void end(Player owner) {
        var ref = ACTIVE.remove(owner);
        WoodGolemEntity golem = ref == null ? null : ref.get();
        if (golem != null && !golem.isRemoved()) {
            golem.dissolve();
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            end(player);
        }
    }

    private static @Nullable WoodGolemEntity current(Player owner) {
        var ref = ACTIVE.get(owner);
        WoodGolemEntity golem = ref == null ? null : ref.get();
        return golem == null || golem.isRemoved() ? null : golem;
    }

    private static boolean locate(ServerLevel level, Player owner, WoodGolemEntity golem) {
        Vec3 axis = KamuiStep.facing(owner.getYRot());
        for (double reach : new double[]{3.8, 5.4}) {
            Vec3 spawn = owner.position().add(axis.scale(reach));
            for (int rise : new int[]{0, 1, -1}) {
                golem.setPos(spawn.x, Math.floor(spawn.y) + rise, spawn.z);
                AABB box = golem.getBoundingBox();
                if (supported(level, owner, box) && level.noCollision(golem, box)
                        && level.clip(new ClipContext(owner.getEyePosition(), golem.position().add(0, 1.8, 0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS
                        && level.getEntities(owner, box, actor -> actor.isAlive() && !actor.isSpectator() && actor.isPickable()).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean supported(ServerLevel level, Player owner, AABB box) {
        var border = level.getWorldBorder();
        if (!KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())) {
            return false;
        }
        for (double x : new double[]{box.minX + 0.05, box.maxX - 0.05}) {
            for (double z : new double[]{box.minZ + 0.05, box.maxZ - 0.05}) {
                BlockPos foot = BlockPos.containing(x, box.minY, z);
                BlockPos ground = foot.below();
                if (!level.hasChunkAt(foot) || !level.mayInteract(owner, foot) || level.isOutsideBuildHeight(ground)
                        || level.isOutsideBuildHeight(BlockPos.containing(x, box.maxY, z))
                        || !level.getBlockState(foot).getFluidState().isEmpty()
                        || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
                    return false;
                }
            }
        }
        return true;
    }
}
