package com.sekwah.narutomod.item;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * A Rinnegan, taken from the corpse of a Mangekyo wielder who had transplanted one.
 * Using it awakens the Six Paths in the holder along with all four paths this mod
 * implements. Unlike the Sharingan and Byakugan the Rinnegan is not a bloodline, so
 * this is deliberately clan-agnostic — anyone who finds one can wield it.
 */
public class RinneganEyeItem extends Item {

    private static final DustParticleOptions RINNEGAN_VIOLET =
            new DustParticleOptions(new Vector3f(0.62f, 0.55f, 0.82f), 1.3f);
    private static final String[] PATHS = {"deva", "preta", "animal", "naraka"};

    public RinneganEyeItem(Properties properties) {
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
            if (ninjaData.isRinneganAwakened()) {
                player.displayClientMessage(
                        Component.translatable("rinnegan.fail.already").withStyle(ChatFormatting.RED), true);
                return;
            }
            ninjaData.setRinneganAwakened(true);
            for (String path : PATHS) {
                ninjaData.unlockRinneganPath(path);
            }
            ref.consumed = true;

            player.displayClientMessage(
                    Component.translatable("rinnegan.awakened").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0f, 0.7f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(RINNEGAN_VIOLET,
                        player.getX(), player.getEyeY(), player.getZ(), 60, 0.5, 0.6, 0.5, 0.02);
            }
        });

        if (ref.consumed) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("rinnegan.eye.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
