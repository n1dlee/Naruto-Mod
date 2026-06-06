package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Soldier Pill — consumable that instantly restores 50 chakra and 30 stamina.
 * Cooldown: 60 seconds.
 */
public class SoldierPillItem extends Item {

    private static final float CHAKRA_RESTORE = 50f;
    private static final float STAMINA_RESTORE = 30f;

    public SoldierPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        var ref = new Object() { boolean used = false; };
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) return;

            ninjaData.addChakra(CHAKRA_RESTORE);
            ninjaData.addStamina(STAMINA_RESTORE);
            ref.used = true;

            player.displayClientMessage(
                    Component.literal("+50 Chakra, +30 Stamina").withStyle(ChatFormatting.GREEN), true);
        });

        if (ref.used) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 60 * 20);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1.2f);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }
}
