package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.sekclib.util.PlayerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class LeapAbility extends Ability {

    /** Exempt from the free-hands gate: this is a jump, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }
    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public boolean logInChat() {
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public SoundEvent castingFailSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(!player.onGround()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notonground", Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if(ninjaData.getStamina() < 10) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina", Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useStamina(10, 40);
        return true;
    }

    /**
     * Leaps in whichever direction the caster is holding, not only the way they are facing.
     *
     * A dodge you can only perform forwards is not a dodge - you cannot break away from
     * something you have to keep your eyes on. Holding A, S or D now throws you sideways or
     * backwards while your aim stays on the target; with no movement key held it falls back
     * to the old look-direction leap, so nothing about the existing feel changes.
     *
     * A sideways or backwards hop is deliberately flatter and shorter than the forward
     * bound: it is an evasion, not travel.
     */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        float[] leapScales = {2.0f, 2.5f, 3.2f, 4.0f, 5.5f};
        int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
        float horScale = leapScales[rank];
        float vertBoost = 0.8f + rank * 0.1f;

        float strafe = ninjaData.getMoveStrafe();
        float forward = ninjaData.getMoveForward();

        if (strafe == 0f && forward == 0f) {
            Vec3 look = player.getLookAngle();
            PlayerUtil.setVelocity(player, look.x * horScale,
                    look.y * 0.6 + vertBoost, look.z * horScale, true);
        } else {
            // Movement input is relative to where the player is looking, so rotate it into
            // world space exactly the way Entity.getInputVector does for walking. Note the
            // signs: getting the one on the forward term wrong leaves the leap correct while
            // you face along Z and fully reversed while you face along X, which reads as
            // "W jumps backwards" and is not obviously a maths error at all.
            double yaw = Math.toRadians(player.getYRot());
            double sin = Math.sin(yaw);
            double cos = Math.cos(yaw);
            double worldX = strafe * cos - forward * sin;
            double worldZ = forward * cos + strafe * sin;
            Vec3 direction = new Vec3(worldX, 0, worldZ).normalize();

            // An evasive hop covers less ground and stays lower than a committed forward leap.
            boolean purelyEvasive = forward <= 0f;
            float dodgeScale = purelyEvasive ? horScale * 0.75f : horScale;
            float dodgeLift = purelyEvasive ? vertBoost * 0.65f : vertBoost;
            PlayerUtil.setVelocity(player, direction.x * dodgeScale, dodgeLift,
                    direction.z * dodgeScale, true);
        }

        player.level().playSound(null,
                player, NarutoSounds.LEAP.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        player.level().gameEvent(player, NarutoGameEvents.LEAP.get(), player.position());
    }
}
