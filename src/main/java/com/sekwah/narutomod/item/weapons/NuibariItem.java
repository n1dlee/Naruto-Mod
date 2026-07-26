package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.item.NinjaTier;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Nuibari — the "longsword" of the Seven Swordsmen, shaped like a giant sewing needle.
 * Canon: it pierces everything in its path and stitches the victims together with its
 * thread. RMB skewers every enemy along a straight line, then binds them: each one is
 * dragged toward the next in the chain and rooted in place.
 */
public class NuibariItem extends SwordItem {

    private static final double THREAD_RANGE = 12.0;
    private static final double THREAD_WIDTH = 1.2;
    private static final float PIERCE_DAMAGE = 7.0f;
    private static final int STITCH_TICKS = 5 * 20;
    private static final int COOLDOWN_TICKS = 140;

    public NuibariItem(Properties properties) {
        super(NinjaTier.KATANA, 4, -2.0f, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // Everything standing on the needle's line, nearest first — that ordering is what
        // makes the stitch read as a single thread running through the whole group.
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(THREAD_RANGE)).inflate(THREAD_WIDTH * 2);
        List<LivingEntity> pierced = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive())) {
            Vec3 center = candidate.position().add(0, candidate.getBbHeight() * 0.5, 0);
            double projection = center.subtract(eye).dot(look);
            if (projection < 0 || projection > THREAD_RANGE) {
                continue;
            }
            if (center.distanceTo(eye.add(look.scale(projection))) <= THREAD_WIDTH) {
                pierced.add(candidate);
            }
        }
        if (pierced.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        pierced.sort(Comparator.comparingDouble(e -> e.position().distanceTo(eye)));

        for (int i = 0; i < pierced.size(); i++) {
            LivingEntity target = pierced.get(i);
            target.hurt(player.damageSources().playerAttack(player), PIERCE_DAMAGE);
            // Stitched: rooted and weakened while the thread holds
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STITCH_TICKS, 3, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STITCH_TICKS, 0, false, true));
            // ...and pulled toward whoever is next on the thread
            if (i + 1 < pierced.size()) {
                Vec3 toNext = pierced.get(i + 1).position().subtract(target.position()).normalize().scale(0.35);
                target.setDeltaMovement(target.getDeltaMovement().add(toNext));
                target.hurtMarked = true;
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            // The thread itself, drawn from the wielder out to the far end of the line
            for (int step = 1; step <= (int) THREAD_RANGE * 2; step++) {
                Vec3 point = eye.add(look.scale(step * 0.5));
                serverLevel.sendParticles(NarutoParticles.METAL_GRAY,
                        point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 1.6f);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.nuibari.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
