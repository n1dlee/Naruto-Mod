package com.sekwah.narutomod.events;

import com.sekwah.narutomod.util.WoodHouseLayout;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.DoorBlock;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodHouseBlueprintTest {
    @Test
    void packagedLegacyHouseDataFixesAndPreservesItsRotatedDoors() throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try (var in = Objects.requireNonNull(getClass().getResourceAsStream("/data/narutomod/structures/wood_house.nbt"))) {
            var old = NbtIo.readCompressed(in);
            var fixed = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), old, old.getInt("DataVersion"));
            Vec3i size = new Vec3i(16, 11, 17);
            var site = WoodHouseLayout.site(BlockPos.ZERO, Direction.EAST, size);
            var blueprint = WoodHouseBlueprint.fromTag(BuiltInRegistries.BLOCK.asLookup(), site, size, fixed);
            var original = WoodHouseLayout.site(BlockPos.ZERO, Direction.NORTH, size);
            var unturned = WoodHouseBlueprint.fromTag(BuiltInRegistries.BLOCK.asLookup(), original, size, fixed);
            assertNotNull(blueprint);
            assertNotNull(unturned);
            assertTrue(blueprint.cells().size() >= 1_100);
            assertTrue(blueprint.cells().stream().noneMatch(cell -> cell.state().isAir()));
            assertEquals(12, blueprint.cells().stream().filter(cell -> cell.state().getBlock() instanceof DoorBlock).count());
            var rotated = blueprint.cells().stream().collect(Collectors.toMap(WoodHouseBlueprint.Cell::pos, Function.identity()));
            for (var cell : unturned.cells()) {
                if (cell.state().getBlock() instanceof DoorBlock) {
                    BlockPos local = cell.pos().subtract(original.corner());
                    BlockPos at = site.corner().offset(WoodHouseLayout.rotated(local, size, site.rotation()));
                    assertEquals(site.rotation().rotate(cell.state().getValue(DoorBlock.FACING)),
                            rotated.get(at).state().getValue(DoorBlock.FACING));
                }
            }
        }
    }
}
