package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NinjaTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Chakra Blade — a sword that can be charged with chakra via RMB hold.
 * While charged, the next melee hit deals +8 bonus damage.
 * Charging costs 20 chakra. Charge lasts 10 seconds (200 ticks).
 */
public class ChakraBladeItem extends SwordItem {

    private static final float CHAKRA_COST = 20f;
    private static final int CHARGE_DURATION = 200; // 10 seconds
    private static final float BONUS_DAMAGE = 8.0f;
    // NBT key for remaining charged ticks
    private static final String TAG_CHARGED = "ChakraCharged";

    public ChakraBladeItem(Properties properties) {
        // Tier: same as katana, +2 attack damage, -2.2 attack speed (slightly slower than katana)
        super(NinjaTier.KATANA, 2, -2.2f, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (isCharged(stack)) {
            player.displayClientMessage(Component.literal("Already charged!").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.pass(stack);
        }

        // Check chakra
        var ref = new Object() { boolean success = false; };
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.getChakra() >= CHAKRA_COST) {
                ninjaData.useChakra(CHAKRA_COST, 10);
                ref.success = true;
            }
        });

        if (ref.success) {
            stack.getOrCreateTag().putInt(TAG_CHARGED, CHARGE_DURATION);
            player.displayClientMessage(Component.literal("Chakra Blade charged!").withStyle(ChatFormatting.AQUA), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);

            // Blue particles
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(0.3f, 0.6f, 1.0f), 1.0f),
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        10, 0.3, 0.5, 0.3, 0.05);
            }
            player.getCooldowns().addCooldown(this, 20); // 1 second cooldown
            return InteractionResultHolder.consume(stack);
        } else {
            player.displayClientMessage(Component.literal("Not enough chakra!").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isCharged(stack)) {
            // Consume charge and deal bonus damage
            stack.getOrCreateTag().putInt(TAG_CHARGED, 0);
            target.hurt(attacker.damageSources().magic(), BONUS_DAMAGE);

            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(0.3f, 0.7f, 1.0f), 1.5f),
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.4, 0.4, 0.4, 0.1);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Glow effect when charged
        return isCharged(stack) || super.isFoil(stack);
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt(TAG_CHARGED) > 0;
    }

    /**
     * Called via item tick to decay the charge timer.
     * We use inventoryTick for this.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && stack.hasTag()) {
            int charged = stack.getTag().getInt(TAG_CHARGED);
            if (charged > 0) {
                stack.getTag().putInt(TAG_CHARGED, charged - 1);
            }
        }
    }
}
