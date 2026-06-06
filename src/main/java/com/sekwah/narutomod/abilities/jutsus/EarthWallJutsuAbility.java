package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthWallEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EarthWallJutsuAbility extends Ability implements Ability.Cooldown {

    private static final int CHAKRA_COST = 40;
    private static final int WALL_DISTANCE = 2;
    private static final int WALL_WIDTH = 3;
    private static final int WALL_HEIGHT = 3;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 311;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
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
        double yawRad = Math.toRadians(Math.round(player.getYRot() / 90.0) * 90.0);

        int forwardX = (int) Math.round(-Math.sin(yawRad));
        int forwardZ = (int) Math.round(Math.cos(yawRad));
        int perpX = forwardZ;
        int perpZ = -forwardX;

        BlockPos center = player.blockPosition()
                .offset(forwardX * WALL_DISTANCE, 0, forwardZ * WALL_DISTANCE);

        int half = WALL_WIDTH / 2;

        // Build one list per height row for the rising animation
        List<List<BlockPos>> rows = new ArrayList<>();
        for (int height = 0; height < WALL_HEIGHT; height++) {
            List<BlockPos> row = new ArrayList<>();
            for (int side = -half; side <= half; side++) {
                row.add(center.offset(perpX * side, height, perpZ * side));
            }
            rows.add(row);
        }

        // Spawn entity immediately so it can track all blocks placed by the delayed events
        EarthWallEntity wallEntity = new EarthWallEntity(NarutoEntities.EARTH_WALL.get(), player.level());
        wallEntity.setPos(Vec3.atCenterOf(center));
        player.level().addFreshEntity(wallEntity);

        // Place each row with a short delay — bottom first, then middle, then top
        for (int i = 0; i < rows.size(); i++) {
            final List<BlockPos> row = rows.get(i);
            final float pitch = 0.8f + i * 0.1f;
            ninjaData.scheduleDelayedTickEvent((p) -> {
                wallEntity.placeRow(row, Blocks.DIRT);
                p.level().playSound(null, p, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0f, pitch);
            }, 2 + i * 4);
        }
    }
}
