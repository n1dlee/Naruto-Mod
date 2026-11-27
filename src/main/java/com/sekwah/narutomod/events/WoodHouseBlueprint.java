package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.util.WoodHouseLayout;
import com.sekwah.narutomod.util.WoodHouseLayout.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

record WoodHouseBlueprint(Site site, List<Cell> cells) {
    private static final ResourceLocation HOUSE = new ResourceLocation(NarutoMod.MOD_ID, "wood_house");

    static @Nullable WoodHouseBlueprint at(ServerLevel level, Player owner) {
        var template = level.getStructureManager().get(HOUSE).orElse(null);
        if (template == null || !WoodHouseLayout.allowedSize(template.getSize())) {
            return null;
        }
        Vec3i size = template.getSize();
        Site site = WoodHouseLayout.site(owner.blockPosition(), owner.getDirection(), size);
        return fromTag(level.registryAccess().lookupOrThrow(Registries.BLOCK), site, size, template.save(new CompoundTag()));
    }

    static @Nullable WoodHouseBlueprint fromTag(HolderGetter<Block> blocks, Site site, Vec3i size, CompoundTag structure) {
        var states = palette(blocks, structure);
        var rows = structure.getList("blocks", Tag.TAG_COMPOUND);
        if (states.isEmpty() || rows.size() > size.getX() * size.getY() * size.getZ()
                || !structure.getList("entities", Tag.TAG_COMPOUND).isEmpty()) {
            return null;
        }
        var occupied = new HashSet<BlockPos>();
        var cells = new ArrayList<Cell>();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.getCompound(i);
            int stateIndex = row.getInt("state");
            var coordinates = row.getList("pos", Tag.TAG_INT);
            if (coordinates.size() != 3 || stateIndex < 0 || stateIndex >= states.size() || row.contains("nbt")) {
                return null;
            }
            var local = new BlockPos(coordinates.getInt(0), coordinates.getInt(1), coordinates.getInt(2));
            if (!inside(local, size) || !occupied.add(local)) {
                return null;
            }
            BlockState state = states.get(stateIndex);
            if (!state.isAir()) {
                cells.add(new Cell(site.corner().offset(WoodHouseLayout.rotated(local, size, site.rotation())), state.rotate(site.rotation())));
            }
        }
        cells.sort(Comparator.comparingInt(cell -> cell.pos().getY()));
        return cells.isEmpty() || !groundFloor(cells, site) ? null : new WoodHouseBlueprint(site, List.copyOf(cells));
    }

    private static List<BlockState> palette(HolderGetter<Block> blocks, CompoundTag structure) {
        var entries = structure.getList("palette", Tag.TAG_COMPOUND);
        var palette = new ArrayList<BlockState>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var state = NbtUtils.readBlockState(blocks, entries.getCompound(i));
            if (!buildingMaterial(state)) {
                return List.of();
            }
            palette.add(state);
        }
        return palette;
    }

    private static boolean buildingMaterial(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        ResourceLocation key = state.getBlock().builtInRegistryHolder().key().location();
        String path = key.getPath();
        return "minecraft".equals(key.getNamespace()) && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && (path.endsWith("_planks") || path.endsWith("_stairs") || path.endsWith("_slab") || path.endsWith("_door")
                || path.endsWith("_trapdoor") || path.endsWith("_fence") || path.equals("torch") || path.equals("wall_torch"));
    }

    private static boolean inside(BlockPos pos, Vec3i size) {
        return pos.getX() >= 0 && pos.getY() >= 0 && pos.getZ() >= 0
                && pos.getX() < size.getX() && pos.getY() < size.getY() && pos.getZ() < size.getZ();
    }

    private static boolean groundFloor(List<Cell> cells, Site site) {
        int feet = site.corner().getY();
        return cells.stream().anyMatch(cell -> cell.pos().getY() == feet);
    }

    record Cell(BlockPos pos, BlockState state) {
    }
}
