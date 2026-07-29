package com.sekwah.narutomod.world;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Kamui's pocket dimension - the black void of floating slabs Obito and Kakashi shunt
 * themselves and their targets into.
 *
 * The dimension itself is data-driven (data/narutomod/dimension/kamui.json plus its
 * dimension_type and biome): a flat generator with zero layers, so the world is genuinely
 * empty, lit only by ambient light with nether-style fog over a pure black biome colour.
 * That is the 1.20 equivalent of the 1.12.2 mod's WorldProvider, which did the same thing
 * in code - empty chunk provider, black fog vector, no sky, no respawn, no weather.
 *
 * What this class owns is everything the JSON cannot express: getting an entity in and out,
 * and raising a slab under them when they arrive so they do not fall forever. The original
 * built one solid pillar from bedrock to y64 on entry; this keeps the silhouette but builds
 * a bounded slab plus a few neighbours, so the place reads as the anime's field of floating
 * blocks instead of one lonely column.
 */
public final class KamuiDimension {

    public static final ResourceKey<Level> KAMUI = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(NarutoMod.MOD_ID, "kamui"));

    /** Slabs sit at this height; everything below is void. */
    public static final int PLATFORM_Y = 64;

    private static final int SLAB_MIN_RADIUS = 4;
    private static final int SLAB_MAX_RADIUS = 7;
    private static final int SLAB_DEPTH = 6;
    private static final int NEIGHBOUR_SLABS = 4;
    private static final int NEIGHBOUR_SPREAD = 40;

    private KamuiDimension() {
    }

    public static boolean isKamui(Level level) {
        return level.dimension().equals(KAMUI);
    }

    /**
     * Sends a player in, remembering where they came from so they can be put back. Returns
     * false if the dimension is missing - a datapack can remove it, and a jutsu that fails
     * cleanly is better than one that throws.
     */
    public static boolean enter(ServerPlayer player, INinjaData ninjaData) {
        ServerLevel target = player.server.getLevel(KAMUI);
        if (target == null) {
            return false;
        }
        ninjaData.setKamuiReturnPoint(player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ());

        // Offset the landing site by the player's identity so two people don't land on top
        // of each other, and so a single player keeps returning to their own island.
        int x = (int) (player.getUUID().getMostSignificantBits() % 2000) * 4;
        int z = (int) (player.getUUID().getLeastSignificantBits() % 2000) * 4;
        BlockPos landing = new BlockPos(x, PLATFORM_Y + 1, z);

        buildIsland(target, landing.below());
        teleport(player, target, landing);
        return true;
    }

    /**
     * Banishes something that is not the caster - the use Obito and Kakashi actually put
     * this to in a fight. The victim keeps no return point of their own, so a mob dropped
     * in here is simply gone, and a player has to find their own way out.
     *
     * Non-players are teleported with changeDimension too rather than being killed, so a
     * boss you could not beat is removed from the fight but still exists.
     */
    public static boolean banish(ServerPlayer caster, Entity victim) {
        ServerLevel target = caster.server.getLevel(KAMUI);
        if (target == null || victim.level().dimension().equals(KAMUI)) {
            return false;
        }
        int x = (int) (caster.getUUID().getMostSignificantBits() % 2000) * 4;
        int z = (int) (caster.getUUID().getLeastSignificantBits() % 2000) * 4;
        BlockPos landing = new BlockPos(x, PLATFORM_Y + 1, z);
        buildIsland(target, landing.below());

        if (victim instanceof ServerPlayer victimPlayer) {
            // A banished player gets a return point so they are not trapped forever, but it
            // is the only mercy: they still have to walk out of a black void.
            victimPlayer.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                    .ifPresent(data -> data.setKamuiReturnPoint(
                            victimPlayer.level().dimension().location().toString(),
                            victimPlayer.getX(), victimPlayer.getY(), victimPlayer.getZ()));
            teleport(victimPlayer, target, landing);
            return true;
        }
        Entity moved = victim.changeDimension(target, new DirectTeleporter(landing));
        return moved != null;
    }

    /**
     * Puts a player back where they entered from. Falls back to the overworld spawn if the
     * stored dimension no longer exists, so nobody is ever stranded in the void.
     */
    public static boolean exit(ServerPlayer player, INinjaData ninjaData) {
        String dimensionId = ninjaData.getKamuiReturnDimension();
        ServerLevel destination = null;
        if (dimensionId != null && !dimensionId.isEmpty()) {
            destination = player.server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(dimensionId)));
        }
        BlockPos pos;
        if (destination == null) {
            destination = player.server.overworld();
            pos = destination.getSharedSpawnPos();
        } else {
            pos = BlockPos.containing(ninjaData.getKamuiReturnX(),
                    ninjaData.getKamuiReturnY(), ninjaData.getKamuiReturnZ());
        }
        teleport(player, destination, pos);
        ninjaData.clearKamuiReturnPoint();
        return true;
    }

    /**
     * Moves an entity across dimensions at an exact position. Forge's ITeleporter hook is
     * the supported way to do this - without it vanilla hunts for or digs a portal at the
     * destination, which would carve nether portals into the void.
     */
    private static void teleport(ServerPlayer player, ServerLevel destination, BlockPos pos) {
        if (player.level().dimension().equals(destination.dimension())) {
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            return;
        }
        player.changeDimension(destination, new DirectTeleporter(pos));
        player.fallDistance = 0f;
    }

    /**
     * Raises the slab the arriving player stands on, plus a scatter of others in view.
     * Idempotent in practice: if the centre is already Kamui stone the island is standing
     * from a previous visit and nothing is rebuilt.
     */
    private static void buildIsland(ServerLevel level, BlockPos centre) {
        BlockState kamui = NarutoBlocks.KAMUI_BLOCK.get().defaultBlockState();
        if (level.getBlockState(centre).is(NarutoBlocks.KAMUI_BLOCK.get())) {
            return;
        }
        RandomSource random = level.getRandom();
        buildSlab(level, centre, kamui, SLAB_MIN_RADIUS + random.nextInt(SLAB_MAX_RADIUS - SLAB_MIN_RADIUS + 1));

        for (int i = 0; i < NEIGHBOUR_SLABS; i++) {
            BlockPos offset = centre.offset(
                    random.nextInt(NEIGHBOUR_SPREAD * 2) - NEIGHBOUR_SPREAD,
                    random.nextInt(24) - 12,
                    random.nextInt(NEIGHBOUR_SPREAD * 2) - NEIGHBOUR_SPREAD);
            buildSlab(level, offset, kamui,
                    SLAB_MIN_RADIUS + random.nextInt(SLAB_MAX_RADIUS - SLAB_MIN_RADIUS + 1));
        }
    }

    private static void buildSlab(ServerLevel level, BlockPos centre, BlockState state, int radius) {
        for (int dy = 0; dy < SLAB_DEPTH; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = centre.offset(dx, -dy, dz);
                    if (pos.getY() < level.getMinBuildHeight() || !level.getBlockState(pos).is(Blocks.AIR)) {
                        continue;
                    }
                    level.setBlock(pos, state, Block_FLAG_SILENT);
                }
            }
        }
    }

    /** Update neighbours but skip client block-update packets for every one of ~1500 blocks. */
    private static final int Block_FLAG_SILENT = 2;

    /**
     * Drops the entity at a fixed spot instead of letting vanilla look for a portal.
     */
    private record DirectTeleporter(BlockPos pos) implements ITeleporter {
        @Nullable
        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                                  float yaw, Function<Boolean, Entity> repositionEntity) {
            Entity placed = repositionEntity.apply(false);
            placed.teleportTo(this.pos.getX() + 0.5, this.pos.getY(), this.pos.getZ() + 0.5);
            placed.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            placed.fallDistance = 0f;
            return placed;
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
            return false; // the jutsu plays its own
        }
    }
}
