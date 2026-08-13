package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthWallEntity;
import com.sekwah.narutomod.util.EyeTargeting;
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
 * Wood Release: Wood Locking Wall - the cage Hashirama grows around someone to take them
 * out of a fight without killing them.
 *
 * It does no damage at all: the point is containment, and anything sealed inside is slowed
 * to a crawl and pinned down for as long as the wood stands.
 *
 * The timber is tracked by an EarthWallEntity rather than a delayed player tick event.
 * That matters: the old scheduling hung the cleanup off the CASTER, so a player who logged
 * out or died in the next ten seconds left the cage standing forever. The tracker entity
 * lives in the world beside the blocks it placed and takes them back regardless.
 */
public class WoodReleaseAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 90f;
    private static final double RANGE = 20.0;
    /** Half-width of the cage, so 2 gives a 5x5 shell with room to stand inside. */
    private static final int RADIUS = 2;
    private static final int HEIGHT = 4;
    private static final int LIFESPAN = 400; // 20 seconds

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Wood grows; it does not appear. */
    @Override
    public int castPoseTicks() {
        return 18;
    }

    @Override
    public long defaultCombo() {
        return 3312;
    }

    @Override
    public String requiredClan() {
        return "senju";
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 6;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 6;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AZALEA_PLACE;
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
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos centre = target.blockPosition();

        EarthWallEntity cage = new EarthWallEntity(NarutoEntities.EARTH_WALL.get(), player.level());
        cage.setPos(Vec3.atCenterOf(centre));
        cage.setLifespan(LIFESPAN);
        player.level().addFreshEntity(cage);
        cage.placeWall(hollowShell(centre), Blocks.OAK_LOG);

        // Everything caught inside is pinned down rather than hurt. The absurd Jump
        // amplifier is the standard trick for "cannot jump at all" - there is no vanilla
        // rooting effect, and a wooden cage you could hop out of would be pointless.
        for (LivingEntity caught : player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre).inflate(RADIUS, HEIGHT * 0.5, RADIUS), e -> e != player && e.isAlive())) {
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LIFESPAN, 3, false, true));
            caught.addEffect(new MobEffectInstance(MobEffects.JUMP, LIFESPAN, 128, false, false));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, Vec3.atCenterOf(centre).subtract(0, 1, 0),
                    RADIUS + 0.5, 0.25, 40, NarutoParticles.LOG_BROWN);
            serverLevel.playSound(null, centre, SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 1.4f, 0.7f);
        }
    }

    /**
     * The cage's surface only - walls, floor and roof, but nothing in the middle, or the
     * technique would bury the target inside solid wood instead of containing them.
     */
    private List<BlockPos> hollowShell(BlockPos centre) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos base = centre.below();
        for (int dy = 0; dy <= HEIGHT; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (dy == 0 || dy == HEIGHT || Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS) {
                        positions.add(base.offset(dx, dy, dz));
                    }
                }
            }
        }
        return positions;
    }
}
