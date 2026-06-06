package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ShadowCloneJutsuAbility extends Ability implements Ability.Cooldown {

    private static final int CHAKRA_COST = 20;
    private static final int CLONE_COUNT = 3;
    private static final double SPREAD = 1.5;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 122;
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
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
        spawnClones(player, CLONE_COUNT, SPREAD);
    }

    protected static void spawnClones(Player player, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double offsetX = (player.getRandom().nextDouble() * 2 - 1) * spread;
            double offsetZ = (player.getRandom().nextDouble() * 2 - 1) * spread;
            ShadowCloneEntity clone = new ShadowCloneEntity(NarutoEntities.SHADOW_CLONE.get(), player.level());
            clone.setOwner(player);
            clone.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
            clone.setYRot(player.getYRot());
            clone.setYBodyRot(player.getYRot());
            clone.setYHeadRot(player.getYHeadRot());
            player.level().addFreshEntity(clone);
        }
    }
}
