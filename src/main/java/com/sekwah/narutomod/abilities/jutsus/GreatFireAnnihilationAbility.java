package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Fire Style: Great Fire Annihilation (Katon: Gouka Mekkyaku) - combo 3113.
 *
 * The top of the fire tree, and Madara's signature Katon: not a projectile but a sustained
 * wall of flame poured out in a wide arc, sweeping everything in front of the caster for
 * several seconds. Where Phoenix Flower is a burst, this is a held torrent - you point it
 * and everything in the cone burns for as long as it lasts.
 *
 * Sets entities alight but never places fire blocks: a technique this wide would otherwise
 * take a forest, and a base, with it.
 */
public class GreatFireAnnihilationAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 85f;
    /** How long the torrent pours, in ticks. */
    private static final int DURATION = 50;
    private static final double RANGE = 14.0;
    /** Cosine of the cone half-angle - roughly 40 degrees each side of the aim line. */
    private static final double CONE_DOT = 0.77;
    private static final float DAMAGE_PER_WAVE = 5.0f;
    /** Ticks between damage waves; the flame is continuous, the burn ticks are not. */
    private static final int WAVE_INTERVAL = 5;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A long exhale, held for as long as the flame pours. */
    @Override
    public int castPoseTicks() {
        return 26;
    }

    @Override
    public long defaultCombo() {
        return 3113;
    }

    @Override
    public int getCooldown() {
        return 35 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BLAZE_SHOOT;
    }

    @Override
    public String element() {
        return "fire";
    }

    @Override
    public int elementLevelRequired() {
        return 14;
    }

    @Override
    public float elementXpReward() {
        return 45f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        float waveDamage = DAMAGE_PER_WAVE * ninjaData.getRankDamageMultiplier()
                * ("uchiha".equals(ninjaData.getClanId()) ? 1.25f : 1.0f);

        for (int tick = 0; tick < DURATION; tick++) {
            final int step = tick;
            ninjaData.scheduleDelayedTickEvent(caster -> pour(caster, step, waveDamage), 6 + tick);
        }
    }

    /** One tick of the torrent: the visible flame every tick, the burn on the wave beat. */
    private void pour(Player caster, int step, float waveDamage) {
        Vec3 origin = caster.getEyePosition();
        Vec3 aim = caster.getLookAngle();

        if (caster.level() instanceof ServerLevel serverLevel) {
            // The cone is drawn as widening rings of flame rather than a straight line, so it
            // reads as a torrent with real volume instead of a laser.
            for (int distance = 2; distance <= (int) RANGE; distance += 2) {
                Vec3 centre = origin.add(aim.scale(distance));
                double spread = distance * 0.28;
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        centre.x, centre.y, centre.z, 12, spread, spread * 0.7, spread, 0.02);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        centre.x, centre.y + 0.4, centre.z, 3, spread, spread * 0.5, spread, 0.01);
            }
            if (step % 10 == 0) {
                serverLevel.playSound(null, caster.blockPosition(), SoundEvents.FIRE_AMBIENT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 2.2f, 0.6f);
            }
        }

        if (step % WAVE_INTERVAL != 0) {
            return;
        }
        AABB reach = caster.getBoundingBox().expandTowards(aim.scale(RANGE)).inflate(RANGE * 0.5);
        for (LivingEntity caught : caster.level().getEntitiesOfClass(LivingEntity.class, reach,
                entity -> entity != caster && entity.isAlive())) {
            Vec3 toward = caught.position().add(0, caught.getBbHeight() * 0.5, 0).subtract(origin);
            if (toward.lengthSqr() > RANGE * RANGE || toward.normalize().dot(aim) < CONE_DOT) {
                continue;
            }
            caught.hurt(caster.damageSources().playerAttack(caster), waveDamage);
            caught.setSecondsOnFire(8);
        }
    }
}
