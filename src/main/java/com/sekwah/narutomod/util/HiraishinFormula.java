package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public final class HiraishinFormula {
    private static final String KEY = "hiraishinFormulas";
    public static final String KUNAI_OWNER = "HiraishinFormulaOwner";

    private HiraishinFormula() {
    }

    public record Anchor(UUID owner, ResourceLocation dimension) {
        public boolean belongsTo(UUID player, ResourceLocation world) {
            return owner.equals(player) && dimension.equals(world);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("owner", owner);
            tag.putString("dimension", dimension.toString());
            return tag;
        }

        @Nullable
        private static Anchor read(CompoundTag tag) {
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
            return tag.hasUUID("owner") && dimension != null && tag.contains("dimension", Tag.TAG_STRING)
                    && !tag.getString("dimension").isEmpty() ? new Anchor(tag.getUUID("owner"), dimension) : null;
        }
    }

    public record Ground(Anchor anchor, BlockPos support, ResourceLocation block) {
        public Ground {
            support = support.immutable();
        }
    }

    public record Brand(Anchor anchor, UUID target) {
    }

    public record Book(@Nullable Ground ground, @Nullable Brand brand, @Nullable BlockPos oldGround, String oldBrand) {
        public static final Book EMPTY = new Book(null, null, null, "");

        public boolean hasLegacy() {
            return oldGround != null || !oldBrand.isEmpty();
        }

        public Book withGround(Ground value) {
            return new Book(value, brand, null, oldBrand);
        }

        public Book withBrand(Brand value) {
            return new Book(ground, value, oldGround, "");
        }

        public void save(CompoundTag root) {
            CompoundTag book = new CompoundTag();
            if (ground != null) {
                CompoundTag entry = ground.anchor().save();
                entry.putLong("support", ground.support().asLong());
                entry.putString("block", ground.block().toString());
                book.put("ground", entry);
            }
            if (brand != null) {
                CompoundTag entry = brand.anchor().save();
                entry.putUUID("target", brand.target());
                book.put("brand", entry);
            }
            if (oldGround != null) {
                book.putLong("oldGround", oldGround.asLong());
            }
            if (!oldBrand.isEmpty()) {
                book.putString("oldBrand", oldBrand);
            }
            root.put(KEY, book);
        }

        public static Book read(CompoundTag root) {
            if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
                return new Book(null, null, legacyGround(root), root.getString("hiraishinEntityMark"));
            }
            CompoundTag book = root.getCompound(KEY);
            return new Book(readGround(book.getCompound("ground")), readBrand(book.getCompound("brand")),
                    book.contains("oldGround", Tag.TAG_LONG) ? BlockPos.of(book.getLong("oldGround")) : null,
                    book.getString("oldBrand"));
        }

        @Nullable
        private static BlockPos legacyGround(CompoundTag root) {
            return root.getBoolean("hasThunderGodMark") && root.contains("thunderGodX", Tag.TAG_INT)
                    && root.contains("thunderGodY", Tag.TAG_INT) && root.contains("thunderGodZ", Tag.TAG_INT)
                    ? new BlockPos(root.getInt("thunderGodX"), root.getInt("thunderGodY"), root.getInt("thunderGodZ")) : null;
        }

        @Nullable
        private static Ground readGround(CompoundTag tag) {
            Anchor anchor = Anchor.read(tag);
            String blockId = tag.getString("block");
            ResourceLocation block = blockId.isEmpty() ? null : ResourceLocation.tryParse(blockId);
            return anchor != null && block != null && tag.contains("support", Tag.TAG_LONG)
                    ? new Ground(anchor, BlockPos.of(tag.getLong("support")), block) : null;
        }

        @Nullable
        private static Brand readBrand(CompoundTag tag) {
            Anchor anchor = Anchor.read(tag);
            return anchor != null && tag.hasUUID("target") ? new Brand(anchor, tag.getUUID("target")) : null;
        }
    }
}
