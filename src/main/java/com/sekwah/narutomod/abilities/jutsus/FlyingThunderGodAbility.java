package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Flying Thunder God — the seal half of the technique (combo 1213, INSTANT).
 *
 * Canon: Minato never teleported to a place, he teleported to his *seal*. So casting this
 * applies a seal, and what gets sealed depends on the situation:
 *
 *   - holding a plain kunai  -> one is converted into a Hiraishin kunai to throw
 *   - looking at a creature  -> the seal is branded onto that creature
 *   - otherwise              -> the seal is laid on the ground where you stand
 *
 * Jumping to any of those is {@link HiraishinTeleportAbility}'s job.
 *
 * S-rank: not granted by rank at all — only by the scroll found in Ancient Cities and
 * bastions (see events/NarutoLootEvents).
 */
public class FlyingThunderGodAbility extends Ability implements Ability.Cooldown {

    /** Exempt from the free-hands gate: this is a movement burst, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    private static final float SEAL_COST = 50f;
    private static final double BRAND_RANGE = 6.0D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The whole point is that nobody sees the movement. */
    @Override
    public int castPoseTicks() {
        return 3;
    }

    @Override
    public long defaultCombo() {
        return 1213;
    }

    @Override
    public int getCooldown() {
        return 2 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.END_PORTAL_FRAME_FILL;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Erasing your own seals is not a technique, it is letting go of one - free.
        if (player.isShiftKeyDown()) {
            return true;
        }
        if (ninjaData.getChakra() < SEAL_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(SEAL_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // 0. Sneaking clears your seals instead of laying another.
        //
        // There was no way to take a seal back, and the branded-creature one is the worst
        // case: HiraishinTeleportAbility prefers a branded creature over a ground seal, so
        // one misaimed cast at a passing cow quietly hijacked every jump afterwards, and the
        // only escape was to spend fifty chakra branding something else.
        if (player.isShiftKeyDown()) {
            clearSeals(player, ninjaData);
            return;
        }

        // 1. A kunai in hand becomes a marked kunai — the classic use.
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(NarutoItems.KUNAI.get())) {
                held.shrink(1);
                ItemStack marked = new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get());
                if (!player.getInventory().add(marked)) {
                    player.drop(marked, false);
                }
                sealEffect(player, player.getX(), player.getY() + 1.0, player.getZ());
                player.displayClientMessage(
                        Component.translatable("hiraishin.seal.kunai").withStyle(ChatFormatting.GOLD), true);
                return;
            }
        }

        // 2. Otherwise brand whatever is in front of you.
        LivingEntity target = EyeTargeting.raycastLiving(player, BRAND_RANGE);
        if (target != null) {
            ninjaData.setHiraishinEntityMark(target.getUUID().toString());
            sealEffect(player, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
            player.displayClientMessage(Component.translatable("hiraishin.seal.entity",
                    target.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
            return;
        }

        // 3. Nothing in hand, nothing in front — seal the ground underfoot.
        BlockPos here = player.blockPosition();
        ninjaData.setThunderGodMark(here);
        sealEffect(player, here.getX() + 0.5, here.getY() + 0.1, here.getZ() + 0.5);
        player.displayClientMessage(Component.translatable("hiraishin.seal.position",
                here.getX(), here.getY(), here.getZ()).withStyle(ChatFormatting.GOLD), true);
    }

    /**
     * Wipes the branded creature and the ground seal.
     *
     * Thrown Hiraishin kunai are deliberately left alone: those are real items lying in the
     * world, and you take one back by walking over and picking it up. Erasing them from here
     * would delete the player's property from a distance.
     */
    private static void clearSeals(Player player, INinjaData ninjaData) {
        boolean hadEntity = ninjaData.getHiraishinEntityMark() != null
                && !ninjaData.getHiraishinEntityMark().isEmpty();
        boolean hadGround = ninjaData.getThunderGodMark() != null;

        if (!hadEntity && !hadGround) {
            player.displayClientMessage(
                    Component.translatable("hiraishin.clear.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ninjaData.setHiraishinEntityMark("");
        ninjaData.setThunderGodMark(null);

        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.7f, 1.5f);
        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, player.position().add(0, 1.0, 0), 18, 0.6,
                    NarutoParticles.TELEPORT_GOLD);
        }
        player.displayClientMessage(
                Component.translatable("hiraishin.clear.done").withStyle(ChatFormatting.GOLD), true);
    }

    private static void sealEffect(Player player, double x, double y, double z) {
        player.level().playSound(null, x, y, z, SoundEvents.END_PORTAL_FRAME_FILL,
                SoundSource.PLAYERS, 0.8f, 1.4f);
        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, new Vec3(x, y, z), 0.8, 24, NarutoParticles.TELEPORT_GOLD);
        }
    }
}
