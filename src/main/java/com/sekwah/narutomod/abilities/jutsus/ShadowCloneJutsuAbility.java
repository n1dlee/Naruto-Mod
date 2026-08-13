package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShadowCloneJutsuAbility extends Ability implements Ability.Cooldown {

    private static final int CHAKRA_COST = 20;
    private static final int CLONE_COUNT = 3;
    private static final double SPREAD = 1.5;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The cross seal is the single most recognisable pose in the series. */
    @Override
    public int castPoseTicks() {
        return 12;
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
        ninjaData.setCrossSealPose(true);
        spawnCasterBurst(player, CLONE_COUNT);
        spawnClones(player, CLONE_COUNT, SPREAD);
    }

    /**
     * Chakra-poof at the caster's own position, sized proportionally to how many clones are
     * about to spawn — so Multiple Shadow Clone (20) visibly reads as a much bigger cast than
     * the base 3-clone version, instead of the two looking identical from the caster's POV.
     */
    protected static void spawnCasterBurst(Player player, int count) {
        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, player.position().add(0, player.getBbHeight() * 0.5, 0),
                    10 + count * 4, 0.3 + count * 0.03, ParticleTypes.POOF);
        }
    }

    protected static void spawnClones(Player player, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double offsetX = (player.getRandom().nextDouble() * 2 - 1) * spread;
            double offsetZ = (player.getRandom().nextDouble() * 2 - 1) * spread;
            ShadowCloneEntity clone = new ShadowCloneEntity(NarutoEntities.SHADOW_CLONE.get(), player.level());
            clone.setOwner(player);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    clone.setItemSlot(slot, stack.copy());
                }
            }
            clone.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
            clone.setYRot(player.getYRot());
            clone.setYBodyRot(player.getYRot());
            clone.setYHeadRot(player.getYHeadRot());
            player.level().addFreshEntity(clone);

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        clone.getX(), clone.getY() + clone.getBbHeight() * 0.5, clone.getZ(),
                        20, 0.25, 0.35, 0.25, 0.05);
            }
        }
    }
}
