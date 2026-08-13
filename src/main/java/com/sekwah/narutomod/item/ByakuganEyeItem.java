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
 * A Byakugan taken from a fallen Hyuga.
 *
 * The Sharingan already had a transplant route and the Rinnegan had an item, but the
 * Byakugan had neither: outside the Hyuga bloodline there was simply no way to ever get
 * one, which quietly made half the dojutsu system unreachable for most characters.
 *
 * Transplanting grants the eye at its lowest level. It does not come with the clan's
 * training, so the range and the techniques still have to be earned - and a born Hyuga
 * gains nothing from a second one.
 */
public class ByakuganEyeItem extends Item {

    public ByakuganEyeItem(Properties properties) {
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
            if ("hyuga".equals(ninjaData.getClanId())) {
                player.displayClientMessage(
                        Component.translatable("byakugan.transplant.fail.hyuga").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (ninjaData.getByakuganLevel() > 0) {
                player.displayClientMessage(
                        Component.translatable("byakugan.transplant.fail.already").withStyle(ChatFormatting.RED), true);
                return;
            }

            ninjaData.setByakuganLevel(1);
            ref.consumed = true;

            // Surgery, same as the Sharingan: it hurts and it leaves you reeling.
            player.hurt(player.damageSources().magic(), 6.0f);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));

            player.displayClientMessage(
                    Component.translatable("byakugan.transplant.done").withStyle(ChatFormatting.AQUA), false);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                    SoundSource.PLAYERS, 1.0f, 0.6f);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 eye = player.getEyePosition();
                serverLevel.sendParticles(com.sekwah.narutomod.util.NarutoParticles.ROTATION_WHITE,
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
        tooltip.add(Component.translatable("byakugan.eye.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
