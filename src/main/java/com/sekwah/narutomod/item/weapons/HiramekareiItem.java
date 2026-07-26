package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NinjaTier;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hiramekarei — the twin-blade that stores chakra and releases it as an enormous
 * construct. Hold RMB to pour chakra into the blade, release to fire the stored charge
 * forward as a hammer-shaped shockwave. Damage and reach both scale with how long it
 * was charged, and the chakra is drawn from the wielder as they hold.
 */
public class HiramekareiItem extends SwordItem {

    private static final int MAX_CHARGE_TICKS = 40;
    private static final float CHAKRA_PER_TICK = 2.0f;
    private static final float MIN_DAMAGE = 6.0f;
    private static final float MAX_DAMAGE = 26.0f;
    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 9.0;
    private static final int COOLDOWN_TICKS = 100;

    public HiramekareiItem(Properties properties) {
        super(NinjaTier.KATANA, 5, -2.4f, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Charging costs chakra every tick; running dry simply stops the charge. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int held = this.getUseDuration(stack) - remainingUseTicks;
        if (held > MAX_CHARGE_TICKS) {
            return;
        }
        var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
        if (data.isPresent()) {
            if (data.get().getChakra() < CHAKRA_PER_TICK) {
                player.stopUsingItem();
                return;
            }
            data.get().useChakra(CHAKRA_PER_TICK, 5);
        }
        if (level instanceof ServerLevel serverLevel && held % 4 == 0) {
            serverLevel.sendParticles(NarutoParticles.WATER_BLUE,
                    player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.4, 0.3, 0.4, 0.01);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int held = Math.min(this.getUseDuration(stack) - remainingUseTicks, MAX_CHARGE_TICKS);
        if (held < 8) {
            return; // a tap does not build a construct
        }
        float charge = held / (float) MAX_CHARGE_TICKS;
        float damage = MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * charge;
        double range = MIN_RANGE + (MAX_RANGE - MIN_RANGE) * charge;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(range)).inflate(range * 0.4),
                e -> e != player && e.isAlive())) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            double projection = toTarget.dot(look);
            if (projection < 0 || projection > range) {
                continue;
            }
            if (toTarget.subtract(look.scale(projection)).length() > 2.2) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 push = look.scale(1.0 + charge).add(0, 0.3, 0);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            // The released construct: a widening column of chakra along the swing
            for (int step = 1; step <= (int) range; step++) {
                Vec3 point = eye.add(look.scale(step));
                serverLevel.sendParticles(NarutoParticles.WATER_BLUE,
                        point.x, point.y, point.z, 12, 0.3 + step * 0.08, 0.3 + step * 0.08, 0.3 + step * 0.08, 0.02);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 1.2f, 0.6f);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.hiramekarei.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
