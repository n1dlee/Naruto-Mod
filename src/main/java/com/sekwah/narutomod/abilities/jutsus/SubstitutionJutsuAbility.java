package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.SubstitutionLogEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SubstitutionFxPacket;
import com.sekwah.narutomod.util.SubstitutionRoute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SubstitutionJutsuAbility extends Ability implements Ability.Channeled {
    public static final int MAX_MARKER_DISTANCE = SubstitutionRoute.MAX_MARKER_DISTANCE;
    private static final int HIDDEN_TICKS = 5;

    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 12;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (chargeAmount == -1 && !player.isCreative() && data.getSubstitutionCount() < 1) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughcharges",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    @Override
    public String getTranslationKey(INinjaData data, int ticksActive) {
        String key = super.getTranslationKey(data, ticksActive);
        return ticksActive <= 1 ? key : key + ".mark";
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (ticksActive < 0) {
            this.performTap(player, data);
        } else {
            data.setSubstitutionLoc(player.position().add(0, SubstitutionRoute.MARKER_LIFT, 0),
                    player.level().dimension().location());
        }
    }

    @Override
    public boolean performTap(Player player, INinjaData data) {
        if (!(player instanceof ServerPlayer actor) || !(player.level() instanceof ServerLevel level)
                || !player.isAlive() || player.isSpectator() || player.isPassenger()
                || !this.handleCost(player, data, -1)) {
            return false;
        }
        Vec3 original = player.position();
        Vec3 marker = data.getSubstitutionLoc();
        boolean prepared = SubstitutionRoute.usesMarker(original, marker, level.dimension().location(),
                data.getSubstitutionDimension());
        Vec3 exit = prepared && marker != null
                ? SubstitutionRoute.safeAt(level, player, marker.subtract(0, SubstitutionRoute.MARKER_LIFT, 0), 0.24)
                : findFallback(level, player);
        if (exit == null || original.distanceToSqr(exit) < 0.75 * 0.75) {
            player.displayClientMessage(Component.translatable("jutsu.substitution.no_safe_exit"), true);
            return false;
        }
        actor.connection.teleport(exit.x, exit.y, exit.z, actor.getYRot(), actor.getXRot());
        if (actor.position().distanceToSqr(exit) > 1.0e-6) {
            return false;
        }
        actor.resetFallDistance();
        if (!player.isCreative()) {
            data.useSubstitution(1);
        }
        if (prepared) {
            data.setSubstitutionLoc(null, null);
        }
        data.setInvisibleTicks(HIDDEN_TICKS);
        actor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, HIDDEN_TICKS, 0, false, false));
        decoy(level, actor, original, exit);
        return true;
    }

    private static void decoy(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        var log = new SubstitutionLogEntity(level);
        log.moveTo(from.x, from.y + 0.12, from.z, player.getYRot(), 0f);
        Vec3 kick = from.subtract(to).multiply(1, 0, 1).normalize().scale(0.13);
        log.setDeltaMovement(kick.x, 0.31, kick.z);
        level.addFreshEntity(log);
        PacketHandler.sendNear(new SubstitutionFxPacket(level.dimension().location(), from, to, level.random.nextInt()),
                level, from.lerp(to, 0.5), 32.0 + from.distanceTo(to) * 0.5);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.87f, 0.83f);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.47f, 1.28f);
    }

    @Nullable
    private static Vec3 findFallback(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 9.0 + level.random.nextDouble() * 3.5;
            Vec3 seed = player.position().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Vec3 exit = SubstitutionRoute.safeAt(level, player, seed, 4.0);
            if (exit != null) {
                return exit;
            }
        }
        return null;
    }

    @Override
    public boolean useChargedMessages() {
        return true;
    }

    @Override
    public boolean channelCommittedAt(int ticksChanneled) {
        return false;
    }
}
