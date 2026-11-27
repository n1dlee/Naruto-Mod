package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.util.HiraishinFormula;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

/**
 * A thrown Hiraishin kunai. Behaves like a normal kunai in flight and sticks where it
 * lands, but carries the Flying Thunder God seal — HiraishinTeleportAbility looks for
 * these to decide where the caster snaps to.
 *
 * An embedded blade remains an anchor for its normal projectile lifetime.
 */
public class HiraishinKunaiEntity extends KunaiEntity {
    private UUID formulaOwner;
    private Direction impactFace = Direction.UP;

    public HiraishinKunaiEntity(EntityType<? extends HiraishinKunaiEntity> type, Level level) {
        super(type, level);
    }

    public HiraishinKunaiEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.HIRAISHIN_KUNAI.get(), shooter, level);
        this.formulaOwner = shooter.getUUID();
    }

    @Override
    protected ItemStack getPickupItem() {
        ItemStack stack = new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get());
        UUID owner = this.formulaOwner;
        if (owner == null && this.getOwner() != null) {
            owner = this.getOwner().getUUID();
        }
        if (owner != null) {
            stack.getOrCreateTag().putUUID(HiraishinFormula.KUNAI_OWNER, owner);
        }
        return stack;
    }

    public void bindFormula(UUID owner) {
        this.formulaOwner = owner;
    }

    public boolean isFormulaFor(LivingEntity actor) {
        return this.getOwner() == actor && (this.formulaOwner == null || this.formulaOwner.equals(actor.getUUID()));
    }

    public boolean isPlanted() {
        return this.inGround;
    }

    public Direction impactFace() {
        return this.impactFace;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        this.impactFace = hit.getDirection();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.formulaOwner != null) {
            tag.putUUID(HiraishinFormula.KUNAI_OWNER, this.formulaOwner);
        }
        tag.putByte("HiraishinImpactFace", (byte) this.impactFace.get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.formulaOwner = tag.hasUUID(HiraishinFormula.KUNAI_OWNER)
                ? tag.getUUID(HiraishinFormula.KUNAI_OWNER) : null;
        this.impactFace = tag.contains("HiraishinImpactFace")
                ? Direction.from3DDataValue(tag.getByte("HiraishinImpactFace")) : Direction.UP;
    }
}
