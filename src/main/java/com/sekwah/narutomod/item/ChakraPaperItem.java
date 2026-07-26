package com.sekwah.narutomod.item;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chakra induction paper — infuse it with chakra and it reacts to reveal a new nature:
 * ignites (fire), gets damp (water), crumbles (earth), splits (wind) or wrinkles
 * (lightning). Awakens a RANDOM still-locked nature, limited by rank slots: everyone
 * starts with their clan nature, a second slot opens at Jonin, a third at Kage.
 */
public class ChakraPaperItem extends Item {

    private static final float CHAKRA_COST = 50f;

    private static final DustParticleOptions EARTH_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.4f, 0.2f), 1.1f);
    private static final DustParticleOptions LIGHTNING_SPARK = new DustParticleOptions(new Vector3f(0.9f, 0.95f, 0.4f), 1.0f);

    public ChakraPaperItem(Properties properties) {
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
                player.displayClientMessage(Component.translatable("jutsu.not_a_ninja").withStyle(ChatFormatting.RED), true);
                return;
            }
            int slots = ninjaData.getMaxElementSlots();
            List<String> unlocked = ninjaData.getUnlockedElements();
            if (unlocked.size() >= slots) {
                player.displayClientMessage(Component.translatable("chakra_paper.fail.slots",
                        Component.literal(String.valueOf(slots)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
                return;
            }
            if (ninjaData.getChakra() < CHAKRA_COST) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.YELLOW)), true);
                return;
            }

            List<String> locked = new ArrayList<>();
            for (String element : NinjaData.ALL_ELEMENTS) {
                if (!ninjaData.isElementUnlocked(element)) {
                    locked.add(element);
                }
            }
            if (locked.isEmpty()) {
                return;
            }
            String awakened = locked.get(player.getRandom().nextInt(locked.size()));
            if (!ninjaData.unlockElement(awakened)) {
                return;
            }
            ninjaData.useChakra(CHAKRA_COST, 20);
            ref.consumed = true;

            // The paper's canonical reaction, per element
            player.displayClientMessage(Component.translatable("chakra_paper.reaction." + awakened)
                    .withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.translatable("chakra_paper.unlocked",
                    Component.translatable("element.narutomod." + awakened).withStyle(elementColor(awakened)))
                    .withStyle(ChatFormatting.GREEN), false);

            if (level instanceof ServerLevel serverLevel) {
                spawnReactionParticles(serverLevel, player, awakened);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    reactionSound(awakened), SoundSource.PLAYERS, 0.8f, 1.0f);
        });

        if (ref.consumed) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    private static ChatFormatting elementColor(String element) {
        return switch (element) {
            case "fire" -> ChatFormatting.RED;
            case "water" -> ChatFormatting.AQUA;
            case "earth" -> ChatFormatting.GOLD;
            case "wind" -> ChatFormatting.GREEN;
            case "lightning" -> ChatFormatting.YELLOW;
            default -> ChatFormatting.WHITE;
        };
    }

    private static net.minecraft.sounds.SoundEvent reactionSound(String element) {
        return switch (element) {
            case "fire" -> SoundEvents.FIRECHARGE_USE;
            case "water" -> SoundEvents.BUCKET_EMPTY;
            case "earth" -> SoundEvents.GRAVEL_BREAK;
            case "wind" -> SoundEvents.PHANTOM_FLAP;
            case "lightning" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            default -> SoundEvents.BOOK_PAGE_TURN;
        };
    }

    private static void spawnReactionParticles(ServerLevel level, Player player, String element) {
        double x = player.getX();
        double y = player.getY() + 1.4;
        double z = player.getZ();
        ParticleOptions particle = switch (element) {
            case "fire" -> ParticleTypes.FLAME;
            case "water" -> ParticleTypes.SPLASH;
            case "earth" -> EARTH_DUST;
            case "wind" -> ParticleTypes.CLOUD;
            case "lightning" -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.CRIT;
        };
        level.sendParticles(particle, x, y, z, 30, 0.35, 0.4, 0.35, 0.06);
        if ("lightning".equals(element)) {
            level.sendParticles(LIGHTNING_SPARK, x, y, z, 16, 0.3, 0.35, 0.3, 0.03);
        }
    }
}
