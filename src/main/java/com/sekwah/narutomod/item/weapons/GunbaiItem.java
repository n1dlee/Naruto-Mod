package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.util.GunbaiMotion;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class GunbaiItem extends SwordItem {
    private static final int SWEEP_COOLDOWN = 100;
    private static final int GUARD_COOLDOWN = 75;

    public GunbaiItem(Properties properties) {
        super(com.sekwah.narutomod.item.NinjaTier.KATANA, 6, -2.8f, properties);
    }

    @Nullable
    public static InteractionHand heldHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof GunbaiItem) {
            return InteractionHand.MAIN_HAND;
        }
        return player.getOffhandItem().getItem() instanceof GunbaiItem ? InteractionHand.OFF_HAND : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this) || player.isSpectator()) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            if (!level.isClientSide) {
                player.getCooldowns().addCooldown(this, GUARD_COOLDOWN);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide) {
            float power = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                    .filter(data -> data.hasSignatureForm("madara")).map(data -> 1.25f).orElse(1.0f);
            if (!GustFrontEntity.fan(player, player.getLookAngle(), hand, power, false)) {
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(this, SWEEP_COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack item) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack item) {
        return GunbaiMotion.GUARD;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.gunbai.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
