package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ice Release: Demonic Mirroring Ice Crystals (combo 2313).
 *
 * Raises the ring in front of the caster and puts them inside it. What the mirrors give is
 * position, not damage: sneak-use steps to the next pane in the ring, so the caster is never
 * where the last attack came from, and never where the next one is going to.
 *
 * The mirrors are entities and they can be broken - by the person trapped inside as much as
 * by anyone else. That is deliberate. A technique that simply wins for twenty seconds is not
 * a fight, and Haku's own was beaten exactly this way.
 */
public class IceMirrorsAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 90f;
    private static final float STEP_COST = 12f;
    /** How far ahead the ring is centred, so the caster is not standing on its rim. */
    private static final double CAST_REACH = 2.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Raising eight mirrors is a full sequence of seals, not a flick. */
    @Override
    public int castPoseTicks() {
        return 22;
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

    /**
     * Short, because the same cast does two different jobs.
     *
     * Raising the ring and stepping between panes both go through performServer, and there is
     * no per-cast hook to give them different cooldowns - an override for one would have been
     * a method nothing calls. What limits the technique is chakra: ninety to raise a ring,
     * twelve to move inside one, against a ring that only stands for twenty seconds.
     */
    @Override
    public int getCooldown() {
        return 2 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.GLASS_PLACE;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Stepping between panes is the cheap half of the technique; raising them is not.
        float cost = hasStandingMirrors(player) && player.isShiftKeyDown() ? STEP_COST : CHAKRA_COST;
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
        java.util.List<IceMirrorEntity> standing = standingMirrors(player);

        if (!standing.isEmpty() && player.isShiftKeyDown()) {
            stepToNextMirror(player, standing);
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 centre = player.position().add(new Vec3(look.x, 0, look.z).normalize().scale(CAST_REACH));
        IceMirrorEntity.raiseRing(player, centre);
        // Put the caster inside their own ring, which is where the technique is fought from.
        player.teleportTo(centre.x, player.getY(), centre.z);
    }

    /** Moves the caster to whichever surviving pane they are not currently standing at. */
    private void stepToNextMirror(Player player, java.util.List<IceMirrorEntity> standing) {
        IceMirrorEntity furthest = null;
        double best = -1;
        for (IceMirrorEntity mirror : standing) {
            double distance = mirror.distanceToSqr(player);
            if (distance > best) {
                best = distance;
                furthest = mirror;
            }
        }
        if (furthest != null) {
            player.teleportTo(furthest.getX(), furthest.getY(), furthest.getZ());
            player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_HIT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
        }
    }

    private static boolean hasStandingMirrors(Player player) {
        return !standingMirrors(player).isEmpty();
    }

    /** This caster's own panes, near enough to still count as their ring. */
    private static java.util.List<IceMirrorEntity> standingMirrors(Player player) {
        return player.level().getEntitiesOfClass(IceMirrorEntity.class,
                player.getBoundingBox().inflate(IceMirrorEntity.RING_RADIUS + 6.0),
                mirror -> mirror.isAlive()
                        && mirror.getOwnerUUID().map(player.getUUID()::equals).orElse(false));
    }
}
