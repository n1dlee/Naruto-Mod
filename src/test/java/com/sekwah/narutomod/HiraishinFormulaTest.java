package com.sekwah.narutomod;

import com.sekwah.narutomod.util.HiraishinFormula.Anchor;
import com.sekwah.narutomod.util.HiraishinFormula.Book;
import com.sekwah.narutomod.util.HiraishinFormula.Brand;
import com.sekwah.narutomod.util.HiraishinFormula.Ground;
import com.sekwah.narutomod.network.s2c.HiraishinPacket;
import com.sekwah.narutomod.util.HiraishinTargeting.Glyph;
import com.sekwah.narutomod.util.HiraishinTargeting.Kind;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiraishinFormulaTest {
    private static final UUID OWNER = new UUID(4, 17);
    private static final UUID TARGET = new UUID(5, 43);
    private static final ResourceLocation WORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation NETHER = new ResourceLocation("minecraft", "the_nether");
    private static final BlockPos POS = new BlockPos(-59, 71, 15);
    private static final Ground GROUND = new Ground(new Anchor(OWNER, WORLD), POS, new ResourceLocation("minecraft", "stone"));

    @Test
    void keepsDifferentDimensionsAndImmutableSupportOnReload() {
        BlockPos.MutableBlockPos source = POS.mutable();
        Ground marked = new Ground(GROUND.anchor(), source, GROUND.block());
        source.set(0, 0, 0);
        Book book = Book.EMPTY.withGround(marked).withBrand(new Brand(new Anchor(OWNER, NETHER), TARGET));
        CompoundTag root = new CompoundTag();
        book.save(root);
        assertEquals(book, Book.read(root));
        assertEquals(POS, Book.read(root).ground().support());
        assertEquals(NETHER, Book.read(root).brand().anchor().dimension());
    }

    @Test
    void rejectsForeignOwnerAndMatchingCoordinatesInAnotherDimension() {
        assertTrue(GROUND.anchor().belongsTo(OWNER, WORLD));
        assertFalse(GROUND.anchor().belongsTo(OWNER, NETHER));
        assertFalse(GROUND.anchor().belongsTo(TARGET, WORLD));
    }

    @Test
    void archivesLegacyEntriesWithoutActivatingAndReplacesOnlyItsOwnKind() {
        CompoundTag old = new CompoundTag();
        old.putBoolean("hasThunderGodMark", true);
        old.putInt("thunderGodX", POS.getX());
        old.putInt("thunderGodY", POS.getY());
        old.putInt("thunderGodZ", POS.getZ());
        old.putString("hiraishinEntityMark", TARGET.toString());
        Book book = Book.read(old);
        assertTrue(book.hasLegacy());
        assertNull(book.ground());
        assertNull(book.brand());
        assertEquals(POS, book.oldGround());
        book = book.withGround(GROUND);
        assertNull(book.oldGround());
        assertEquals(TARGET.toString(), book.oldBrand());
        CompoundTag saved = new CompoundTag();
        book.save(saved);
        assertEquals(book, Book.read(saved));
        assertFalse(book.withBrand(new Brand(GROUND.anchor(), TARGET)).hasLegacy());
    }

    @Test
    void malformedVersionedEntriesNeverBecomeActiveAnchors() {
        CompoundTag root = new CompoundTag();
        Book.EMPTY.withGround(GROUND).withBrand(new Brand(GROUND.anchor(), TARGET)).save(root);
        var book = root.getCompound("hiraishinFormulas");
        book.getCompound("ground").putString("dimension", "invalid world:key");
        book.getCompound("brand").remove("owner");
        assertEquals(Book.EMPTY, Book.read(root));
    }

    @Test
    void previewPacketDoesNotLoseThePlaneOrTrackedEntity() {
        var expected = new HiraishinPacket(NETHER, 103, HiraishinPacket.Stage.PREVIEW,
                new Glyph(Kind.CREATURE, new Vec3(-57.25, 83.5, 29), Direction.WEST, 370));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HiraishinPacket.encode(expected, buffer);
            assertEquals(expected, HiraishinPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
