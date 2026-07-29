package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ice Release: Ice Spear Barrage - Haku's needles, thrown as a scattered volley.
 *
 * Built on the water bullet projectile rather than a bespoke ice entity: mechanically a
 * frozen water needle IS a water bullet with a tighter, colder profile, and reusing the
 * existing projectile means it already handles ownership, damage typing and block
 * collision correctly. Each shard is individually weak - the technique's damage is in
 * putting several on target at once.
 */
public class IceSpearBarrageAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 50f;
    private static final int SHARDS = 6;
    private static final float SHARD_POWER = 0.55f;
    private static final double SPREAD = 0.12;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 2312;
    }

    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public String secondaryElement() {
        return "wind";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 8;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.POWDER_SNOW_BREAK;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 look = player.getLookAngle().normalize();
        float power = SHARD_POWER * ninjaData.getRankDamageMultiplier();

        for (int i = 0; i < SHARDS; i++) {
            Vec3 aim = look.add(
                    (player.getRandom().nextDouble() - 0.5) * SPREAD,
                    (player.getRandom().nextDouble() - 0.5) * SPREAD,
                    (player.getRandom().nextDouble() - 0.5) * SPREAD).normalize();
            WaterBulletJutsuEntity shard =
                    new WaterBulletJutsuEntity(player, aim.x, aim.y, aim.z);
            shard.setDamageMultiplier(power);
            player.level().addFreshEntity(shard);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    player.getEyePosition().add(look.scale(1.2)), 25, 0.6, NarutoParticles.ICE_PALE);
        }
    }
}
