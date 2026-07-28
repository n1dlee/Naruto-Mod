package com.sekwah.narutomod.item;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Bingo Book — the shinobi bounty ledger. Use it to draw a mission: hunt down N of a
 * target mob for a chakra-XP reward (kill tracking lives in PlayerEvents). Use it again
 * while a bounty is active to check progress. Higher-value marks pay better.
 */
public class BingoBookItem extends Item {

    /** target entity type, kill count, chakra XP reward */
    private record Bounty(EntityType<?> target, int count, float rewardXp) {}

    private static final Bounty[] BOUNTIES = {
            new Bounty(EntityType.ZOMBIE, 15, 800f),
            new Bounty(EntityType.SKELETON, 15, 900f),
            new Bounty(EntityType.SPIDER, 12, 700f),
            new Bounty(EntityType.CREEPER, 10, 1200f),
            new Bounty(EntityType.WITCH, 5, 1500f),
            new Bounty(EntityType.ENDERMAN, 5, 2000f)
    };

    public BingoBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.getBountyTargetId().isEmpty() && ninjaData.getBountyRemaining() > 0) {
                player.displayClientMessage(Component.literal("Bounty in progress: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(ninjaData.getBountyRemaining() + "x " + prettyName(ninjaData.getBountyTargetId()))
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" remaining").withStyle(ChatFormatting.GOLD)), false);
                return;
            }

            Bounty bounty = BOUNTIES[player.getRandom().nextInt(BOUNTIES.length)];
            String targetId = EntityType.getKey(bounty.target()).toString();
            ninjaData.setBounty(targetId, bounty.count(), bounty.rewardXp());
            player.displayClientMessage(Component.literal("New bounty: eliminate ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(bounty.count() + "x " + prettyName(targetId)).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" - reward " + (int) bounty.rewardXp() + " chakra XP").withStyle(ChatFormatting.GREEN)), false);
        });
        return InteractionResultHolder.consume(stack);
    }

    public static String prettyName(String entityTypeId) {
        int colon = entityTypeId.indexOf(':');
        String name = colon >= 0 ? entityTypeId.substring(colon + 1) : entityTypeId;
        return name.replace('_', ' ');
    }
}
