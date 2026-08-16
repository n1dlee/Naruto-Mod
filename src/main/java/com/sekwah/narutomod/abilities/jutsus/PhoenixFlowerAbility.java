package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Fire Style: Phoenix Sage Fire (Katon: Hosenka no Jutsu) - combo 3111.
 *
 * The mid-tier Katon the roster was missing. Great Fireball was the only fire technique in
 * the mod, so a fire-natured ninja had exactly one option from Genin to Kage no matter how
 * far they trained the nature.
 *
 * Where Great Fireball is one heavy shot, this is a spread of small ones fired in quick
 * succession - less damage per hit, far harder to sidestep, and it sets a wide area alight.
 */
public class PhoenixFlowerAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;
    private static final int FLOWER_COUNT = 6;
    /** Ticks between shots; the volley reads as a burst rather than a shotgun blast. */
    private static final int SHOT_INTERVAL = 3;
    /** How far each shot is nudged off the aim line, in radians. */
    private static final double SPREAD = 0.12;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Six seals in quick succession, then the volley - it holds a beat longer than one shot. */
    @Override
    public int castPoseTicks() {
        return 16;
    }

    @Override
    public long defaultCombo() {
        return 3111;
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.FIRECHARGE_USE;
    }

    @Override
    public String element() {
        return "fire";
    }

    @Override
    public int elementLevelRequired() {
        return 6;
    }

    @Override
    public float elementXpReward() {
        return 25f;
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
        // A five-petalled rose in flame, in the plane the volley fans across.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.sekwah.narutomod.util.ElementalVfx.fireBloom(serverLevel,
                    player.getEyePosition().add(player.getLookAngle().scale(0.9)),
                    player.getLookAngle(), 0.75);
        }

        boolean uchiha = "uchiha".equals(ninjaData.getClanId());
        float rankMultiplier = ninjaData.getRankDamageMultiplier();

        for (int shot = 0; shot < FLOWER_COUNT; shot++) {
            final int index = shot;
            ninjaData.scheduleDelayedTickEvent(caster -> {
                Vec3 look = caster.getLookAngle();
                // Fan the volley out around the aim line rather than randomly, so the spread
                // is a readable pattern the target can actually try to move out of.
                double angle = (index - (FLOWER_COUNT - 1) / 2.0) * SPREAD;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vec3 aim = new Vec3(
                        look.x * cos - look.z * sin,
                        look.y + (index % 2 == 0 ? 0.05 : -0.05),
                        look.z * cos + look.x * sin).normalize();

                FireballJutsuEntity flower = new FireballJutsuEntity(caster, aim.x, aim.y, aim.z);
                // Each bloom is a fraction of a charged Great Fireball - the volley as a
                // whole is what does the work.
                flower.setChargeAmount(4, uchiha, rankMultiplier * 0.45f);
                caster.level().addFreshEntity(flower);
                caster.level().playSound(null, caster, SoundEvents.FIRECHARGE_USE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.3f + index * 0.05f);
            }, 4 + shot * SHOT_INTERVAL);
        }
    }
}
