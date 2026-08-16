package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.BlockState;
import com.sekwah.narutomod.util.SpikeField;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.block.IceSpikeBlock;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthWallEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ice Release: Thousand Flying Water Needles of Death - a field of ice spears bursting up
 * out of the ground in front of the caster.
 *
 * Hyoton is not a nature you can awaken; it is what a ninja who has mastered both Water
 * and Wind can do with them at once, so the gate is on both (see Ability.secondaryElement).
 */
public class IceSpikesAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 65f;
    private static final float SPIKE_DAMAGE = 8f;
    /**
     * Four, not three. spikeSegment() tapers BASE -> FRUSTUM -> MIDDLE -> TIP, so a
     * three-block column stopped at MIDDLE and the spear never actually came to a point.
     */
    private static final int SPIKE_HEIGHT = 4;
    private static final int LIFESPAN = 140;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Matches the earth version it is modelled on. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 2311;
    }

    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public String secondaryElement() {
        return "wind";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 8;
    }

    @Override
    public int getCooldown() {
        return 14 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        // A hexagonal dendrite: six arms with sixty-degree side branches, like a real flake.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel vfxLevel) {
            com.sekwah.narutomod.util.ElementalVfx.frostDendrite(vfxLevel, player.position(), 2.2);
        }

        float damage = SPIKE_DAMAGE * ninjaData.getRankDamageMultiplier();

        // A disc around the caster rather than a line ahead of them, sized by Ice mastery to
        // a ceiling of twenty blocks - see SpikeField. This is the technique you use when you
        // are surrounded, and a line was exactly the wrong shape for that.
        // There is no "ice" nature to read: Hyoton is Water and Wind held together, which is
        // what the element gate above already requires. Mastery is therefore the weaker of the
        // two - the technique is only as good as whichever half you have neglected.
        int iceLevel = Math.min(ninjaData.getElementLevel("water"), ninjaData.getElementLevel("wind"));
        double radius = SpikeField.radiusFor(iceLevel);
        List<BlockPos> roots = SpikeField.roots(player.level(), player, radius,
                SpikeField.countFor(iceLevel));

        for (BlockPos root : roots) {
            // The wave travels outward, so the delay follows distance rather than list order.
            int delay = 2 + (int) (Math.sqrt(root.distToCenterSqr(player.position())) * 1.6);
            ninjaData.scheduleDelayedTickEvent(p -> eruptSpike(p, root, damage), delay);
            ninjaData.scheduleDelayedTickEvent(p -> retract(p, root), delay + LIFESPAN);
        }
    }

    private void eruptSpike(Player player, BlockPos root, float damage) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int height = 0; height < SPIKE_HEIGHT; height++) {
            BlockPos pos = root.above(height);
            if (!serverLevel.getBlockState(pos).isAir()) {
                break; // ran into terrain or a build - stop rather than carve through it
            }
            // UPDATE_CLIENTS only, same reason the earth version does it: a full block update
            // would let neighbour physics disturb the tapered column as it is being built.
            serverLevel.setBlock(pos, spikeSegment(height), Block.UPDATE_CLIENTS);
        }

        for (LivingEntity caught : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(root).inflate(1.0, SPIKE_HEIGHT, 1.0), e -> e != player && e.isAlive())) {
            caught.hurt(player.damageSources().playerAttack(player), damage);
            // Ice chills rather than launches: the field is meant to hold ground.
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
            chill(caught);
        }

        NarutoParticles.spawnBurst(serverLevel, Vec3.atCenterOf(root.above(1)), 12, 0.4,
                NarutoParticles.ICE_PALE);
        serverLevel.playSound(null, root, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                0.7f, 1.2f + (float) Math.random() * 0.2f);
    }

    /**
     * Drives the target toward the frozen state powder snow uses.
     *
     * This is vanilla's own freeze track, not a custom effect: the blue vignette, the shivering
     * and the freeze damage all come for free, and armour that protects against powder snow
     * protects against this too, which is the behaviour anyone would expect. Each spike adds to
     * the meter rather than setting it, so walking through a field is what actually freezes you
     * - one glancing spike should chill, not kill.
     */
    private static void chill(LivingEntity caught) {
        int required = caught.getTicksRequiredToFreeze();
        // Capped a little past the freezing threshold so the damage phase starts but the
        // target is not locked solid for a minute after the field is gone.
        int capped = Math.min(caught.getTicksFrozen() + FREEZE_PER_SPIKE, required + 60);
        caught.setTicksFrozen(capped);
    }

    /** How much of the freeze meter one spike is worth. */
    private static final int FREEZE_PER_SPIKE = 70;

    /**
     * The spear's silhouette, bottom to top: the same four-stage taper vanilla dripstone uses,
     * in the mod's own ice block. Stacking cubes of packed ice - which is what this did - is a
     * pillar, not a spear, and read as the player building a wall out of the ground.
     */
    private BlockState spikeSegment(int height) {
        DripstoneThickness thickness = switch (height) {
            case 0 -> DripstoneThickness.BASE;
            case 1 -> DripstoneThickness.FRUSTUM;
            case 2 -> DripstoneThickness.MIDDLE;
            default -> DripstoneThickness.TIP;
        };
        return NarutoBlocks.ICE_SPIKE.get().defaultBlockState()
                .setValue(IceSpikeBlock.THICKNESS, thickness);
    }

    /** Melts the column away again, and only ever removes this mod's own spikes. */
    private void retract(Player player, BlockPos root) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int height = SPIKE_HEIGHT - 1; height >= 0; height--) {
            BlockPos pos = root.above(height);
            if (serverLevel.getBlockState(pos).is(NarutoBlocks.ICE_SPIKE.get())) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
