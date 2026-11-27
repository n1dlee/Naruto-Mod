package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.MirrorTransit;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.MirrorRing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class IceMirrorsAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 90f;
    private static final float STEP_COST = 12f;
    private static final double CAST_REACH = 2.0;
    private static final double TRAP_RANGE = 12.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return MirrorRing.RISE_TICKS + MirrorRing.STAGGER_TICKS;
    }

    @Override
    public long defaultCombo() {
        return 2313;
    }

    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 12;
    }

    @Override
    public String secondaryElement() {
        return "wind";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 12;
    }

    @Override
    public int getCooldown() {
        return 2 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        var ring = player.isShiftKeyDown() ? MirrorTransit.ringNear(player, player.position()) : java.util.List.<IceMirrorEntity>of();
        if (!ring.isEmpty() && MirrorTransit.destination(player, ring) == null) {
            player.displayClientMessage(Component.translatable("jutsu.ice_mirrors.no_exit"), true);
            return false;
        }
        float cost = ring.isEmpty() ? CHAKRA_COST : STEP_COST;
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.isShiftKeyDown()) {
            var ring = MirrorTransit.ringNear(player, player.position());
            if (!ring.isEmpty()) {
                MirrorTransit.travel(player, ring);
                return;
            }
        }
        LivingEntity target = EyeTargeting.raycastLiving(player, TRAP_RANGE);
        Vec3 center = target != null && !target.isSpectator() && !Faction.sameSide(player, target)
                ? target.position() : player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(CAST_REACH));
        var raised = IceMirrorEntity.raiseRing(player, center);
        if (raised.isEmpty()) {
            player.displayClientMessage(Component.translatable("jutsu.ice_mirrors.no_footing"), true);
            return;
        }
        var id = raised.get(0).ringId();
        var level = player.level();
        ninjaData.scheduleDelayedTickEvent(caster -> {
            if (caster.level() == level) {
                MirrorTransit.travel(caster, MirrorTransit.ring(caster, id));
            }
        }, MirrorRing.RISE_TICKS + MirrorRing.STAGGER_TICKS + 1);
    }
}
