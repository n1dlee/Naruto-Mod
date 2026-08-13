package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Chibaku Tensei (combo 1323) - the Deva Path's last word.
 *
 * A core is thrown into the sky over whatever the caster is looking at, and for eight seconds
 * everything under it falls toward it. It is the most expensive technique a player can cast
 * and the one with the longest wind-down, because what it does is take an area away from the
 * other side rather than deal a number.
 *
 * The core leaves the landscape alone. Every technique in this mod does; these get cast next
 * to whatever someone has built, and a moon made of their house is not a fight anyone wanted.
 */
public class ChibakuTenseiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 220f;
    /** How far ahead the core is planted, and how high above that point it starts. */
    private static final double CAST_RANGE = 14.0;
    private static final double CAST_HEIGHT = 4.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The longest stance in the mod - this is not something anyone throws in passing. */
    @Override
    public int castPoseTicks() {
        return 34;
    }

    @Override
    public long defaultCombo() {
        return 1323;
    }

    /** The Rinnegan, and nothing else. */
    @Override
    public String requiredEye() {
        return "rinnegan";
    }

    @Override
    public int getCooldown() {
        return 60 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.END_PORTAL_SPAWN;
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
        Vec3 look = player.getLookAngle();
        Vec3 ground = player.getEyePosition().add(look.scale(CAST_RANGE));
        Vec3 origin = new Vec3(ground.x, ground.y + CAST_HEIGHT, ground.z);

        player.level().addFreshEntity(new ChibakuTenseiEntity(player, origin));

        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, origin, 3.0, -0.2, 32, NarutoParticles.SHADOW_PURPLE);
        }
    }
}
