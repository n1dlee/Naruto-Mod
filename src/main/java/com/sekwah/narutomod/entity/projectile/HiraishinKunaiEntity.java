package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A thrown Hiraishin kunai. Behaves like a normal kunai in flight and sticks where it
 * lands, but carries the Flying Thunder God seal — HiraishinTeleportAbility looks for
 * these to decide where the caster snaps to.
 *
 * It stays put once thrown (no despawn) because the whole point is placing a waypoint on
 * the battlefield and using it later.
 */
public class HiraishinKunaiEntity extends KunaiEntity {

    public HiraishinKunaiEntity(EntityType<? extends HiraishinKunaiEntity> type, Level level) {
        super(type, level);
    }

    public HiraishinKunaiEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.HIRAISHIN_KUNAI.get(), shooter, level);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get());
    }

    @Override
    public void tick() {
        super.tick();
        // A faint marker glow so a planted kunai is findable in a fight.
        if (!this.level().isClientSide && this.inGround && this.tickCount % 10 == 0
                && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(NarutoParticles.TELEPORT_GOLD,
                    this.getX(), this.getY() + 0.2, this.getZ(), 2, 0.08, 0.08, 0.08, 0.0);
        }
    }
}
