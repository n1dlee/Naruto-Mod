package com.sekwah.narutomod.item;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A Sharingan cut from a dead Uchiha. Transplanting it gives a non-Uchiha the dojutsu —
 * Kakashi's whole deal — at the canon price: the eye is not theirs, so it never closes.
 * It burns chakra every second for as long as they live with it (NinjaData's transplant
 * upkeep), and driving it in combat costs several times more than it would an Uchiha.
 *
 * A born Uchiha has no use for it; their own eye is already better integrated.
 */
public class SharinganEyeItem extends Item {

    public SharinganEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        var ref = new Object() { boolean consumed = false; };
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                player.displayClientMessage(
                        Component.translatable("jutsu.not_a_ninja").withStyle(ChatFormatting.RED), true);
                return;
            }
            if ("uchiha".equals(ninjaData.getClanId())) {
                player.displayClientMessage(
                        Component.translatable("sharingan.transplant.fail.uchiha").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (ninjaData.isTransplantedSharingan()) {
                player.displayClientMessage(
                        Component.translatable("sharingan.transplant.fail.already").withStyle(ChatFormatting.RED), true);
                return;
            }

            ninjaData.setTransplantedSharingan(true);
            ref.consumed = true;

            // Surgery, not a power-up: it hurts and leaves you reeling.
            player.hurt(player.damageSources().magic(), 6.0f);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));

            player.displayClientMessage(
                    Component.translatable("sharingan.transplant.done").withStyle(ChatFormatting.DARK_RED), false);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                    SoundSource.PLAYERS, 1.0f, 0.6f);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 eye = player.getEyePosition();
                serverLevel.sendParticles(com.sekwah.narutomod.util.NarutoParticles.SHARINGAN_RED,
                        eye.x, eye.y, eye.z, 40, 0.3, 0.3, 0.3, 0.02);
            }
        });

        if (ref.consumed) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("sharingan.eye.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("sharingan.eye.tooltip.cost").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
