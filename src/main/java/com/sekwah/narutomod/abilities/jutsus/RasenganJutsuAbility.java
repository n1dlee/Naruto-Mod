package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenganEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Rasengan - CHANNELED jutsu (combo 212).
 * Hold keys for at least 1 second (20 ticks) to form the Rasengan, then release to fire.
 * Longer charge = stronger knockback (scales 4.0 → 6.0 over 20–60 ticks).
 * Deals 7 hearts (14 HP) to players but cannot kill them (leaves at 1 HP).
 */
public class RasenganJutsuAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    private static final float CHAKRA_PER_TICK = 2.0f;
    private static final int MIN_CHARGE = 20;  // 1 second
    private static final int MAX_CHARGE = 60;  // 3 seconds (knockback caps here)

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 212;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    // Must hold at least MIN_CHARGE ticks — no instant-release cast
    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_TICK, 5);
        return true;
    }

    /**
     * Called every tick while channeling. Spawns a blue spiral around the player's hand.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // Spiral pattern that rotates faster as charge grows
            double speed = 0.7 + (ticksChanneled / 40.0);
            double angle = ticksChanneled * speed;
            double radius = 0.35;

            for (int i = 0; i < 2; i++) {
                double a = angle + Math.PI * i;
                double px = player.getX() + Math.cos(a) * radius;
                double py = player.getEyeY() - 0.35 + Math.sin(ticksChanneled * 0.3) * 0.1;
                double pz = player.getZ() + Math.sin(a) * radius;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        px, py, pz, 1, 0, 0, 0, 0.01);
            }
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Clamp charge amount between min and max
        final int charge = Math.max(MIN_CHARGE, Math.min(ticksActive, MAX_CHARGE));
        ninjaData.scheduleDelayedTickEvent((p) -> {
            Vec3 look = p.getLookAngle();
            RasenganEntity rasengan = new RasenganEntity(p, look.x, look.y, look.z);
            rasengan.setChargeAmount(charge);
            rasengan.setDamageMultiplier(ninjaData.getRankDamageMultiplier());
            rasengan.setCanKillPlayers(ninjaData.getNinjaRank() >= 4);
            p.level().addFreshEntity(rasengan);
        }, 3);
    }
}
