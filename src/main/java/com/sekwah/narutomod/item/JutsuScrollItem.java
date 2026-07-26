package com.sekwah.narutomod.item;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Phase 15 C: a technique scroll — studying it permanently teaches one jutsu.
 * Elemental techniques additionally require their nature to be awakened first
 * (mastery LEVEL is only checked at cast time, knowing a technique and being
 * able to pull it off are different things).
 */
public class JutsuScrollItem extends Item {

    private final String jutsuPath;

    public JutsuScrollItem(String jutsuPath, Properties properties) {
        super(properties);
        this.jutsuPath = jutsuPath;
    }

    public String getJutsuPath() {
        return this.jutsuPath;
    }

    private Ability getAbility() {
        return NarutoRegistries.ABILITIES.getValue(new ResourceLocation("narutomod", this.jutsuPath));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        var ref = new Object() { boolean learned = false; };
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                player.displayClientMessage(Component.translatable("jutsu.not_a_ninja").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (ninjaData.isJutsuLearned(this.jutsuPath)) {
                player.displayClientMessage(Component.translatable("jutsu_scroll.already_known",
                        Component.translatable("narutomod:" + this.jutsuPath).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }
            Ability ability = this.getAbility();
            if (ability != null && ability.element() != null && !ninjaData.isElementUnlocked(ability.element())) {
                player.displayClientMessage(Component.translatable("jutsu_scroll.fail.element",
                        Component.translatable("element.narutomod." + ability.element()).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            ninjaData.learnJutsu(this.jutsuPath);
            ref.learned = true;
            player.displayClientMessage(Component.translatable("jutsu_scroll.learned",
                    Component.translatable("narutomod:" + this.jutsuPath).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GREEN), false);
        });

        if (ref.learned) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.9f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("jutsu_scroll.tooltip.teaches",
                Component.translatable("narutomod:" + this.jutsuPath).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GRAY));
        Ability ability = this.getAbility();
        if (ability != null && ability.element() != null) {
            tooltip.add(Component.translatable("jutsu_scroll.tooltip.element",
                    Component.translatable("element.narutomod." + ability.element()).withStyle(ChatFormatting.AQUA),
                    Component.literal(String.valueOf(ability.elementLevelRequired())).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
