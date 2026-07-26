package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fuma Shuriken (Windmill Shuriken) — a heavier throwing star that arcs back
 * toward its thrower after reaching max range, rather than falling/embedding
 * like a regular shuriken.
 */
public class FumaShurikenEntity extends ShurikenEntity {

    private static final int OUTBOUND_TICKS = 18;
    private static final double RETURN_SPEED = 1.3;

    private boolean returning = false;

    public FumaShurikenEntity(EntityType<? extends ShurikenEntity> type, Level worldIn) {
        super(type, worldIn);
    }

    public FumaShurikenEntity(Level worldIn, LivingEntity shooter) {
        super(NarutoEntities.FUMA_SHURIKEN.get(), shooter, worldIn);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (!this.returning && !this.inGround && this.getRotateTicks() >= OUTBOUND_TICKS) {
            this.returning = true;
        }

        if (this.returning) {
            Entity owner = this.getOwner();
            if (owner == null || !owner.isAlive()) {
                this.discard();
                return;
            }

            Vec3 toOwner = owner.position().add(0, owner.getBbHeight() * 0.5, 0).subtract(this.position());
            if (toOwner.length() < 1.2) {
                this.discard();
                return;
            }

            Vec3 dir = toOwner.normalize().scale(RETURN_SPEED);
            this.setDeltaMovement(dir);
            this.setYRot((float) (Mth.atan2(dir.x, dir.z) * (180D / Math.PI)));
            this.setXRot((float) (Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180D / Math.PI)));
            this.hasImpulse = true;
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(NarutoItems.FUMA_SHURIKEN.get());
    }
}
