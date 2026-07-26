package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/**
 * Rasengan — held in hand (combo 212, TOGGLE), like Naruto actually uses it: formed, carried,
 * resized on the fly, and rammed into a target or a wall rather than thrown. Toggle on to form
 * it, scroll the mouse wheel to grow/shrink it (20-60, same scale the old charge used), melee
 * an entity to slam it into them (see PlayerEvents.applyRasenganMeleeHit), or punch a block to
 * blast a small crater (see PlayerEvents.onLeftClickBlock). Either action consumes it.
 */
public class RasenganJutsuAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {

    private static final float CHAKRA_PER_TICK = 1.5f;
    private static final float ACTIVATE_COST = 15f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 212;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateChakra(player, ninjaData, ACTIVATE_COST);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return validateChakra(player, ninjaData, CHAKRA_PER_TICK);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!ninjaData.isRasenganHeld()) {
            ninjaData.useChakra(ACTIVATE_COST, 10);
            ninjaData.setRasenganHeld(true);
            player.displayClientMessage(
                    Component.literal("Rasengan formed!").withStyle(ChatFormatting.AQUA), true);
        } else {
            ninjaData.useChakra(CHAKRA_PER_TICK, 5);
        }
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setRasenganHeld(false);
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.END_PORTAL_FRAME_FILL;
    }

    /**
     * Spiral + compressing ring in the player's hand, scaled by the current held charge
     * (20-60, adjustable with the scroll wheel).
     */
    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        int charge = ninjaData.getRasenganCharge();
        float t = Math.max(0, Math.min(charge - 20, 40)) / 40.0f;
        double radius = 0.2 + t * 0.25;
        double angle = player.tickCount * (0.7 + t);

        for (int i = 0; i < 2; i++) {
            double a = angle + Math.PI * i;
            double px = player.getX() + Math.cos(a) * radius;
            double py = player.getEyeY() - 0.35 + Math.sin(player.tickCount * 0.3) * 0.1;
            double pz = player.getZ() + Math.sin(a) * radius;
            player.level().addParticle(NarutoParticles.RASENGAN_BLUE, px, py, pz, 0, 0, 0);
        }

        if (player.tickCount % 4 == 0) {
            double ringRadius = radius + 0.15;
            double py = player.getEyeY() - 0.35;
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8 - player.tickCount * 0.15;
                double px = player.getX() + Math.cos(a) * ringRadius;
                double pz = player.getZ() + Math.sin(a) * ringRadius;
                player.level().addParticle(NarutoParticles.RASENGAN_BLUE, px, py, pz, 0, 0, 0);
            }
        }
    }

    private boolean validateChakra(Player player, INinjaData ninjaData, float cost) {
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
