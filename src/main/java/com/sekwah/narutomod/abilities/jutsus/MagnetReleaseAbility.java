package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Magnet Release (Jiton) — combo 3321, INSTANT.
 * Attracts metallic item entities in 8 block radius toward look-direction target.
 * Damages entities at the target point (3 per item, max 5 items).
 * Cost: 40 chakra. Cooldown: 12 seconds.
 */
public class MagnetReleaseAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 40f;
    private static final double RADIUS = 8.0;
    private static final float DAMAGE_PER_ITEM = 3.0f;
    private static final int MAX_ITEMS = 5;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3321;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.ANVIL_LAND;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 15);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 targetPoint = eye.add(look.scale(RADIUS));

        // Find metallic item entities
        AABB area = new AABB(player.position().subtract(RADIUS, RADIUS, RADIUS),
                player.position().add(RADIUS, RADIUS, RADIUS));
        List<ItemEntity> allItems = level.getEntitiesOfClass(ItemEntity.class, area);

        List<ItemEntity> metallicItems = new ArrayList<>();
        for (ItemEntity itemEntity : allItems) {
            if (metallicItems.size() >= MAX_ITEMS) break;
            if (isMetallic(itemEntity.getItem())) {
                metallicItems.add(itemEntity);
            }
        }

        if (metallicItems.isEmpty()) {
            player.displayClientMessage(Component.literal("No metallic items nearby!")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        // Fling items toward target point
        for (ItemEntity item : metallicItems) {
            Vec3 dir = targetPoint.subtract(item.position()).normalize().scale(1.5);
            item.setDeltaMovement(dir);
            item.setNoGravity(true);
            item.hasImpulse = true;

            // Gray metallic particles along trajectory
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        NarutoParticles.METAL_GRAY,
                        item.getX(), item.getY() + 0.25, item.getZ(),
                        3, 0.1, 0.1, 0.1, 0.02);
            }
        }

        // Damage living entities near the target point
        AABB damageBox = new AABB(targetPoint.subtract(2, 2, 2), targetPoint.add(2, 2, 2));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageBox,
                e -> e.isAlive() && e != player);
        float totalDamage = metallicItems.size() * DAMAGE_PER_ITEM * ninjaData.getRankDamageMultiplier();
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), totalDamage);
        }

        // Particles at target — dense metallic field, not just a flat burst
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    NarutoParticles.METAL_GRAY,
                    targetPoint.x, targetPoint.y, targetPoint.z,
                    15, 0.5, 0.5, 0.5, 0.05);
            NarutoParticles.spawnRing(serverLevel, targetPoint, 1.2, 16, NarutoParticles.METAL_GRAY);
            NarutoParticles.spawnRing(serverLevel, targetPoint, 0.6, 10, NarutoParticles.METAL_GRAY);
        }

        // Schedule gravity restoration for items
        ninjaData.scheduleDelayedTickEvent(p -> {
            for (ItemEntity item : metallicItems) {
                if (item.isAlive()) {
                    item.setNoGravity(false);
                }
            }
        }, 20); // 1 second
    }

    private boolean isMetallic(ItemStack stack) {
        if (stack.is(Items.IRON_INGOT) || stack.is(Items.IRON_NUGGET) ||
            stack.is(Items.IRON_BLOCK) || stack.is(Items.CHAIN) ||
            stack.is(Items.IRON_SWORD) || stack.is(Items.IRON_AXE) ||
            stack.is(Items.IRON_PICKAXE) || stack.is(Items.IRON_SHOVEL) ||
            stack.is(Items.IRON_HOE) || stack.is(Items.IRON_HELMET) ||
            stack.is(Items.IRON_CHESTPLATE) || stack.is(Items.IRON_LEGGINGS) ||
            stack.is(Items.IRON_BOOTS) || stack.is(Items.SHIELD)) {
            return true;
        }
        // Any tiered item with iron-level tier
        return stack.getItem() instanceof TieredItem tiered &&
               tiered.getTier().getLevel() == 2; // Iron tier level
    }
}
