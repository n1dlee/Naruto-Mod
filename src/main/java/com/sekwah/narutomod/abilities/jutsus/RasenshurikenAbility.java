package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Wind Style: Rasenshuriken (combo 2122) — the thrown evolution of the Rasengan.
 * Canon requirements carried over: you must already be holding a formed Rasengan
 * (combo 212), and shaping + throwing it safely takes Sage Mode or Kurama Chakra Mode
 * (Naruto could only THROW it after mastering Sage Mode — before that it shredded his
 * own arm). Consumes the held Rasengan on cast.
 */
public class RasenshurikenAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 60f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Shaping it takes visibly longer than an ordinary Rasengan. */
    @Override
    public int castPoseTicks() {
        return 18;
    }

    @Override
    public long defaultCombo() {
        return 2122;
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.PHANTOM_SWOOP;
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "wind";
    }

    @Override
    public int elementLevelRequired() {
        return 12;
    }

    @Override
    public float elementXpReward() {
        return 40f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!ninjaData.isRasenganHeld()) {
            player.displayClientMessage(Component.literal("Form a Rasengan first (it becomes the shuriken)!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!ninjaData.isSageModeActive() && !ninjaData.isKcmActive()) {
            player.displayClientMessage(Component.literal("Throwing it safely needs Sage Mode or Kurama Chakra Mode!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // The held Rasengan IS the projectile — consuming it here also stops the held-mode
        // melee/crater handlers from double-firing.
        ninjaData.setRasenganHeld(false);

        Vec3 look = player.getLookAngle();
        RasenshurikenEntity shuriken = new RasenshurikenEntity(player, look.x, look.y, look.z);
        float multiplier = ninjaData.getRankDamageMultiplier() * (ninjaData.isKcmActive() ? 1.5f : 1.0f);
        shuriken.setDamageMultiplier(multiplier);
        player.level().addFreshEntity(shuriken);
    }
}
