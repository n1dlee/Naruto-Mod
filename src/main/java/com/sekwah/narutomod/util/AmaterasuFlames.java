package com.sekwah.narutomod.util;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Amaterasu's black flame, once it has caught on something living.
 *
 * The 1.12.2 mod modelled this as a custom potion effect whose tick handler dealt damage
 * and refused to be doused. There is no MobEffect registry in this mod, and adding one for
 * a single internal state would mean a registry, an icon and a lang entry for something the
 * player is never meant to brew or cure - so the burn is stored on the entity's Forge
 * persistent data instead. Same behaviour, no new registry.
 *
 * The defining property, and the reason this is not just setSecondsOnFire: water does not
 * put it out. Nothing does, short of the caster calling the flames back or the burn running
 * its course.
 */
public final class AmaterasuFlames {

    private static final String TAG_TICKS = "NarutoAmaterasuTicks";
    private static final String TAG_POWER = "NarutoAmaterasuPower";

    /** Damage lands on this cadence rather than every tick, which would be a stun-lock. */
    private static final int DAMAGE_INTERVAL = 5;
    private static final float BASE_DAMAGE = 2.0f;
    public static final int DEFAULT_DURATION = 200;

    private AmaterasuFlames() {
    }

    public static void ignite(LivingEntity target, int ticks, int power) {
        CompoundTag data = target.getPersistentData();
        // Re-igniting refreshes rather than stacks, and keeps the stronger of the two.
        data.putInt(TAG_TICKS, Math.max(data.getInt(TAG_TICKS), ticks));
        data.putInt(TAG_POWER, Math.max(data.getInt(TAG_POWER), power));
    }

    public static boolean isBurning(LivingEntity entity) {
        return entity.getPersistentData().getInt(TAG_TICKS) > 0;
    }

    public static void extinguish(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(TAG_TICKS);
        data.remove(TAG_POWER);
    }

    /**
     * Called once per tick per living entity from PlayerEvents. Returns quietly for the
     * overwhelming majority of entities, which are not on fire.
     */
    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        int remaining = data.getInt(TAG_TICKS);
        if (remaining <= 0) {
            return;
        }
        data.putInt(TAG_TICKS, remaining - 1);
        int power = data.getInt(TAG_POWER);

        if (entity.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = entity.position();
            serverLevel.sendParticles(NarutoParticles.AMATERASU_BLACK,
                    pos.x, pos.y + entity.getBbHeight() * 0.5, pos.z,
                    3 + power, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.35,
                    entity.getBbWidth() * 0.4, 0.0);
        }

        if (entity.tickCount % DAMAGE_INTERVAL != 0) {
            return;
        }
        if (entity.fireImmune()) {
            // Blaze and friends shrug it off; no point letting the burn tick forever.
            extinguish(entity);
            return;
        }
        entity.invulnerableTime = 0; // the flame is continuous, not a series of blows
        entity.hurt(NarutoDamageTypes.getDamageSource(entity.level(), NarutoDamageTypes.AMATERASU),
                BASE_DAMAGE * (1f + power * 0.5f));
    }
}
