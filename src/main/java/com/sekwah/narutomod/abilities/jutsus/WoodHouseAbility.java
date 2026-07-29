package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Wood Release: Four Pillars House - Hashirama raising a building out of bare ground.
 *
 * The one technique here that is not conjured geometry: it stamps a real prebuilt
 * structure into the world, carried over verbatim from the 1.12.2 mod's own
 * wood_house_2.nbt. Unlike every other Mokuton technique the result is PERMANENT - it is
 * a house, and a house that dissolved after twenty seconds would be pointless. The long
 * cooldown and steep cost are what keep that from being abused.
 */
public class WoodHouseAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 250f;
    private static final ResourceLocation TEMPLATE =
            new ResourceLocation(NarutoMod.MOD_ID, "wood_house");

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3221;
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
        return 10;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 10;
    }

    @Override
    public int getCooldown() {
        return 120 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WOOD_PLACE;
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
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Optional<StructureTemplate> template = serverLevel.getStructureManager().get(TEMPLATE);
        if (template.isEmpty()) {
            player.displayClientMessage(Component.translatable("jutsu.woodhouse.missing")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        StructureTemplate house = template.get();

        // Built in front of the caster and turned to face them, so you end up looking at
        // the front of the house rather than standing inside a wall.
        Rotation rotation = rotationFor(player.getDirection());
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        BlockPos origin = player.blockPosition().offset(
                (int) Math.round(forward.x * 4), 0, (int) Math.round(forward.z * 4));

        // Templates place from a corner, so shift by half the (rotated) footprint to keep
        // the house centred on where the player was actually aiming.
        var size = house.getSize(rotation);
        BlockPos placeAt = origin.offset(-size.getX() / 2, 0, -size.getZ() / 2);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true);
        house.placeInWorld(serverLevel, placeAt, placeAt, settings, serverLevel.getRandom(), 2);

        NarutoParticles.spawnRing(serverLevel, Vec3.atCenterOf(origin), 5.0, 60, NarutoParticles.LOG_BROWN);
        serverLevel.playSound(null, origin, SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    /** Turns the house to face back at whoever grew it. */
    private Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
