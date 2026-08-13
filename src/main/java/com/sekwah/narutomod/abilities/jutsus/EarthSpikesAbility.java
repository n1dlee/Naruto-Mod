package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Earth Style - Earth Spikes (combo 313).
 *
 * Stone spears tear up out of the ground under whoever is in front of the caster, throwing
 * them into the air, and stay standing long enough to be an obstacle.
 *
 * This replaces an earlier version that read as a construction tool rather than a technique,
 * for three reasons worth recording so they don't come back:
 *
 *  - it raised its pillars in a fixed line along the caster's facing, ignoring where anything
 *    actually was, so against a moving target it usually erupted through empty ground;
 *  - it tested for victims exactly once, on the tick a pillar appeared. Anyone who was not
 *    standing on that block in that single frame took nothing at all;
 *  - a spike was three cubes of dirt, which is a wall segment, not a spear.
 *
 * So: the spikes now aim at living targets (falling back to a line only when there is nothing
 * to hit), they taper to a real point, and they hurt on eruption and again while you are stuck
 * on them.
 */
public class EarthSpikesAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 30f;
    private static final int SPIKE_COUNT = 5;
    /** One earth block of root plus three dripstone segments that narrow to a point. */
    private static final int SPIKE_HEIGHT = 4;
    private static final double SPIKE_SPACING = 2.0;
    private static final int LIFESPAN_TICKS = 8 * 20;
    private static final float SPIKE_DAMAGE = 8.0f;

    /** How far ahead the technique looks for something to erupt under. */
    private static final double TARGET_RANGE = 14.0;
    /** Cone the caster's facing has to contain a target within - roughly 66 degrees each side. */
    private static final double CONE_DOT = 0.4;
    /** Horizontal reach of a single spike's hitbox. Generous: it is a spear, not a needle. */
    private static final double SPIKE_REACH = 1.8;
    /** Ticks after eruption at which anything still impaled takes a second, weaker hit. */
    private static final int IMPALE_DELAY = 12;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Palms to the ground while the spikes travel outward. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 313;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 4;
    }

    @Override
    public float elementXpReward() {
        return 20f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        final float spikeDamage = SPIKE_DAMAGE * ninjaData.getRankDamageMultiplier();

        double yawRad = Math.toRadians(Math.round(player.getYRot() / 45.0) * 45.0);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));

        List<BlockPos> spikeRoots = chooseSpikeRoots(player, forward);

        for (int i = 0; i < spikeRoots.size(); i++) {
            final BlockPos root = spikeRoots.get(i);
            final int delay = 2 + i * 3;

            ninjaData.scheduleDelayedTickEvent(caster -> erupt(caster, root, spikeDamage), delay);
            ninjaData.scheduleDelayedTickEvent(caster ->
                    sweep(caster, root, spikeDamage * 0.5f, false), delay + IMPALE_DELAY);
            ninjaData.scheduleDelayedTickEvent(caster -> retract(caster, root), delay + LIFESPAN_TICKS);
        }
    }

    /**
     * Picks where the spikes come up.
     *
     * Living targets in front of the caster come first, closest outward, each led slightly by
     * its own velocity so a running target still gets speared - the eruption is deliberately
     * delayed for the wave effect, and without the lead that delay was exactly what let
     * everything walk out of the way. Any remaining spikes fall back to the old straight line,
     * which is what makes the technique still usable on empty ground or as a wall.
     */
    private List<BlockPos> chooseSpikeRoots(Player player, Vec3 forward) {
        List<BlockPos> roots = new ArrayList<>();
        Level level = player.level();

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(TARGET_RANGE),
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isSpectator()
                        // Your own clones stand right where you are aiming; spearing them is
                        // never what you meant by casting this.
                        && !(entity instanceof ShadowCloneEntity));
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));

        for (LivingEntity target : targets) {
            if (roots.size() >= SPIKE_COUNT) {
                break;
            }
            Vec3 toward = target.position().subtract(player.position());
            Vec3 flat = new Vec3(toward.x, 0, toward.z);
            if (flat.lengthSqr() < 1.0E-4 || flat.normalize().dot(forward) < CONE_DOT) {
                continue;
            }
            int index = roots.size();
            Vec3 lead = target.getDeltaMovement().scale((2 + index * 3) * 0.5);
            roots.add(groundUnder(level, target.getX() + lead.x, target.getY() + 1, target.getZ() + lead.z));
        }

        for (int i = 1; roots.size() < SPIKE_COUNT; i++) {
            double distance = i * SPIKE_SPACING + 1.0;
            roots.add(groundUnder(level,
                    player.getX() + forward.x * distance,
                    player.getY(),
                    player.getZ() + forward.z * distance));
        }
        return roots;
    }

    /** First open block above the ground at this column, searched down from startY. */
    private BlockPos groundUnder(Level level, double x, double startY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int by = (int) Math.floor(startY) + 1;
        while (by > level.getMinBuildHeight() && level.getBlockState(new BlockPos(bx, by, bz)).isAir()) {
            by--;
        }
        return new BlockPos(bx, by + 1, bz);
    }

    /** Raises one spike and hits everything it comes up through. */
    private void erupt(Player caster, BlockPos root, float damage) {
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        sweep(caster, root, damage, true);

        for (int height = 0; height < SPIKE_HEIGHT; height++) {
            BlockPos pos = root.above(height);
            if (!serverLevel.getBlockState(pos).isAir()) {
                break; // ran into terrain or a build - stop rather than carve through it
            }
            // UPDATE_CLIENTS only: a normal block update would run the dripstone's own
            // placement physics and immediately pop the tapered segments back off.
            serverLevel.setBlock(pos, spikeSegment(height), Block.UPDATE_CLIENTS);
        }

        serverLevel.playSound(null, root, SoundEvents.GRAVEL_BREAK,
                SoundSource.BLOCKS, 1.2f, 0.7f + (float) Math.random() * 0.3f);
        serverLevel.playSound(null, root, SoundEvents.POINTED_DRIPSTONE_LAND,
                SoundSource.BLOCKS, 0.9f, 0.6f + (float) Math.random() * 0.2f);

        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK,
                Blocks.COARSE_DIRT.defaultBlockState());
        serverLevel.sendParticles(debris,
                root.getX() + 0.5, root.getY() + SPIKE_HEIGHT * 0.5, root.getZ() + 0.5,
                24, 0.35, SPIKE_HEIGHT * 0.4, 0.35, 0.05);
    }

    /**
     * The spike's silhouette, bottom to top: a block of packed earth at the root, then
     * dripstone narrowing base to frustum to tip. Pointed dripstone is the only vanilla shape
     * that actually comes to a point, and as a bonus it carries vanilla's own impaling
     * fall-damage behaviour for anything that lands on it.
     */
    private BlockState spikeSegment(int height) {
        if (height == 0) {
            return Blocks.PACKED_MUD.defaultBlockState();
        }
        DripstoneThickness thickness = switch (height) {
            case 1 -> DripstoneThickness.BASE;
            case 2 -> DripstoneThickness.FRUSTUM;
            default -> DripstoneThickness.TIP;
        };
        return Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
                .setValue(PointedDripstoneBlock.THICKNESS, thickness);
    }

    /** Damages whatever is standing in this spike's volume. */
    private void sweep(Player caster, BlockPos root, float damage, boolean launch) {
        AABB volume = new AABB(root).inflate(SPIKE_REACH, 0, SPIKE_REACH)
                .expandTowards(0, SPIKE_HEIGHT, 0);
        List<LivingEntity> caught = caster.level().getEntitiesOfClass(LivingEntity.class, volume,
                entity -> entity != caster && entity.isAlive()
                        && !(entity instanceof ShadowCloneEntity));
        for (LivingEntity entity : caught) {
            entity.hurt(caster.damageSources().playerAttack(caster), damage);
            if (launch) {
                Vec3 velocity = entity.getDeltaMovement();
                entity.setDeltaMovement(velocity.x * 0.3, Math.min(velocity.y + 0.8, 1.4), velocity.z * 0.3);
                entity.hurtMarked = true;
            }
        }
    }

    /** Sinks the spike back into the ground, taking away only what this cast put there. */
    private void retract(Player caster, BlockPos root) {
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int height = SPIKE_HEIGHT - 1; height >= 0; height--) {
            BlockPos pos = root.above(height);
            Block block = serverLevel.getBlockState(pos).getBlock();
            if (block == Blocks.POINTED_DRIPSTONE || block == Blocks.PACKED_MUD) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
