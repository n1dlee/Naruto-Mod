package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.LeapFxPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.DoubleJumpMotion;
import com.sekwah.sekclib.util.PlayerUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class DoubleJumpAbility extends Ability {

    /** Exempt from the free-hands gate: this is a jump, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    public static final float CHAKRA_COST = 2f;
    public static final float STAMINA_COST = 5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 0;
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
        return ninjaData.getDoubleJumpData().canDoubleJumpServer && airWindow(player)
                && !ninjaData.isWallWalkAttached() && ninjaData.getChakra() >= CHAKRA_COST
                && ninjaData.getStamina() >= STAMINA_COST;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel level) || !this.handleCost(player, ninjaData, 0)
                || !ninjaData.getDoubleJumpData().consumeServer()) {
            return;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        ninjaData.useStamina(STAMINA_COST, 40);
        player.resetFallDistance();
        Vec3 motion = player.getDeltaMovement();
        double lift = DoubleJumpMotion.lift(motion.y, ninjaData.getNinjaRank());
        PlayerUtil.setVelocity(player, motion.x, lift, motion.z, true);
        Vec3 point = player.position().add(0, 0.035, 0);
        PacketHandler.sendNear(new LeapFxPacket(level.dimension().location(), player.getId(), LeapFxPacket.Phase.BOOST,
                point, Vec3.directionFromRotation(0, player.getYRot()), Block.getId(Blocks.AIR.defaultBlockState()),
                (float) Math.min(lift, 1.0)), level, point, 48.0);

        level.playSound(null, player, NarutoSounds.DOUBLE_JUMP.get(), SoundSource.PLAYERS, 0.62f, 1.23f);

        player.level().gameEvent(player, NarutoGameEvents.DOUBLE_JUMP.get(), player.position());
    }

    public static boolean airWindow(Player player) {
        return DoubleJumpMotion.validWindow(player.onGround(), player.onClimbable(), player.isInWaterOrBubble(),
                player.isSpectator() || player.isPassenger() || player.isFallFlying() || player.isNoGravity()
                        || player.getAbilities().flying || player.hasEffect(MobEffects.LEVITATION))
                && !player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player)
                && player.level().noCollision(player, player.getBoundingBox().move(0, 0.16, 0));
    }
}
