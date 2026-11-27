package com.sekwah.narutomod.world;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.KamuiTransitRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class KamuiDimension {
    public static final ResourceKey<Level> KAMUI = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(NarutoMod.MOD_ID, "kamui"));
    private static final String MOB_ANCHOR = "NarutoKamuiReturn";

    private KamuiDimension() {
    }

    public static boolean isKamui(Level level) {
        return level.dimension().equals(KAMUI);
    }

    @Nullable
    public static LivingEntity enter(LivingEntity traveler, ServerLevel pocket) {
        if (!isKamui(pocket) || isKamui(traveler.level())) {
            return null;
        }
        Anchor origin = new Anchor(traveler.level().dimension().location().toString(), traveler.position());
        var data = traveler.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
        if (traveler instanceof ServerPlayer && data.isEmpty()) {
            return null;
        }
        BlockPos platform = KamuiTransitRoute.island(traveler.getUUID());
        if (!pocket.getWorldBorder().isWithinBounds(platform)) {
            platform = new BlockPos(0, KamuiTransitRoute.PLATFORM_Y, 0);
        }
        buildIsland(pocket, platform);
        Vec3 entry = Vec3.atBottomCenterOf(platform.above()).add(0, 0.02, 0);
        Vec3 safe = KamuiTransitRoute.landing(entry, pos -> supported(pocket, traveler, pos));
        LivingEntity moved = safe == null ? null : move(traveler, pocket, safe);
        if (moved instanceof ServerPlayer) {
            data.ifPresent(value -> value.setKamuiReturnPoint(origin.dimension(), origin.feet().x, origin.feet().y, origin.feet().z));
        } else if (moved instanceof Mob mob) {
            storeMob(mob, origin, pocket.getGameTime() + KamuiTransitRoute.MOB_STAY);
        }
        return moved;
    }

    @Nullable
    public static LivingEntity exit(ServerPlayer player) {
        var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
        Anchor saved = data.map(value -> new Anchor(value.getKamuiReturnDimension(),
                new Vec3(value.getKamuiReturnX(), value.getKamuiReturnY(), value.getKamuiReturnZ())))
                .orElse(new Anchor("", Vec3.ZERO));
        var moved = returnHome(player, player.server, saved);
        if (moved != null) {
            data.ifPresent(value -> value.clearKamuiReturnPoint());
        }
        return moved;
    }

    public static boolean mobReturnDue(Mob mob, long clock) {
        return mob.getPersistentData().contains(MOB_ANCHOR, Tag.TAG_COMPOUND)
                && mob.getPersistentData().getCompound(MOB_ANCHOR).getLong("until") <= clock;
    }

    @Nullable
    public static LivingEntity exitMob(Mob mob, ServerLevel pocket) {
        var tag = mob.getPersistentData().getCompound(MOB_ANCHOR);
        var anchor = new Anchor(tag.getString("dimension"), new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")));
        LivingEntity moved = returnHome(mob, pocket.getServer(), anchor);
        if (moved != null) {
            moved.getPersistentData().remove(MOB_ANCHOR);
        }
        return moved;
    }

    private static void storeMob(Mob mob, Anchor anchor, long deadline) {
        var data = new CompoundTag();
        data.putString("dimension", anchor.dimension());
        data.putDouble("x", anchor.feet().x);
        data.putDouble("y", anchor.feet().y);
        data.putDouble("z", anchor.feet().z);
        data.putLong("until", deadline);
        mob.getPersistentData().put(MOB_ANCHOR, data);
    }

    @Nullable
    private static LivingEntity returnHome(LivingEntity actor, MinecraftServer server, Anchor anchor) {
        ResourceLocation parsed = ResourceLocation.tryParse(anchor.dimension());
        ServerLevel world = parsed == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, parsed));
        if (world != null && !isKamui(world) && finite(anchor.feet()) && loadArea(world, BlockPos.containing(anchor.feet()))) {
            ServerLevel destination = world;
            Vec3 near = KamuiTransitRoute.landing(anchor.feet(), pos -> supported(destination, actor, pos));
            if (near != null) {
                return move(actor, destination, near);
            }
        }
        ServerLevel fallback = server.overworld();
        BlockPos spawn = fallback.getSharedSpawnPos();
        if (!loadArea(fallback, spawn)) {
            return null;
        }
        int top = fallback.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        Vec3 safe = KamuiTransitRoute.landing(new Vec3(spawn.getX() + 0.5, top + 0.03, spawn.getZ() + 0.5),
                pos -> supported(fallback, actor, pos));
        return safe == null ? null : move(actor, fallback, safe);
    }

    private static boolean loadArea(ServerLevel level, BlockPos center) {
        if (!level.getWorldBorder().isWithinBounds(center)) {
            return false;
        }
        for (int x = (center.getX() - 7) >> 4; x <= (center.getX() + 7) >> 4; x++) {
            for (int z = (center.getZ() - 7) >> 4; z <= (center.getZ() + 7) >> 4; z++) {
                level.getChunk(x, z);
            }
        }
        return true;
    }

    private static boolean supported(ServerLevel level, LivingEntity actor, Vec3 feet) {
        return KamuiStep.safe(level, actor, feet)
                && level.getBlockCollisions(actor, KamuiStep.arrivalBounds(actor, feet).move(0, -0.25, 0)).iterator().hasNext();
    }

    @Nullable
    private static LivingEntity move(LivingEntity actor, ServerLevel level, Vec3 position) {
        Entity moved = actor.changeDimension(level, new Transit(position));
        if (!(moved instanceof LivingEntity living) || living.level() != level) {
            return null;
        }
        living.fallDistance = 0;
        living.setDeltaMovement(Vec3.ZERO);
        return living;
    }

    private static void buildIsland(ServerLevel level, BlockPos center) {
        if (!loadArea(level, center) || level.getBlockState(center).is(NarutoBlocks.KAMUI_BLOCK.get())) {
            return;
        }
        slab(level, center, 4, 3);
        slab(level, center.offset(13, -5, 7), 2, 4);
        slab(level, center.offset(-12, 4, -8), 2, 3);
        slab(level, center.offset(7, 8, -13), 2, 3);
    }

    private static void slab(ServerLevel level, BlockPos at, int radius, int depth) {
        var state = NarutoBlocks.KAMUI_BLOCK.get().defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-radius, 1 - depth, -radius), at.offset(radius, 0, radius))) {
            if (level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z)
                && Math.abs(point.x) < 30_000_000 && Math.abs(point.z) < 30_000_000;
    }

    private record Anchor(String dimension, Vec3 feet) {
    }

    private record Transit(Vec3 position) implements ITeleporter {
        @Override
        public PortalInfo getPortalInfo(Entity actor, ServerLevel level, Function<ServerLevel, PortalInfo> vanilla) {
            return new PortalInfo(this.position, Vec3.ZERO, actor.getYRot(), actor.getXRot());
        }

        @Override
        public Entity placeEntity(Entity actor, ServerLevel source, ServerLevel destination, float yaw,
                                  Function<Boolean, Entity> reposition) {
            return reposition.apply(false);
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel source, ServerLevel destination) {
            return false;
        }
    }
}
