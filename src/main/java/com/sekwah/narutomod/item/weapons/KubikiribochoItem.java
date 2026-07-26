package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.item.NinjaTier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kubikiribōchō — Zabuza's decapitating great-sword. Heavy two-handed tier
 * (high damage, slow swing). RMB triggers a sweeping cleave hitting every
 * entity in a cone in front of the wielder, with minor lifesteal on hit.
 */
public class KubikiribochoItem extends SwordItem {

    private static final double CLEAVE_RANGE = 4.0;
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(50));
    private static final float CLEAVE_DAMAGE = 9.0f;
    private static final float LIFESTEAL_FRACTION = 0.10f;

    public KubikiribochoItem(Properties properties) {
        super(NinjaTier.KATANA, 7, -3.2f, properties);
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
        Vec3 look = player.getLookAngle();

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(CLEAVE_RANGE)).inflate(CLEAVE_RANGE * 0.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        float totalDamageDealt = 0;
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            if (toTarget.dot(look) < HALF_ANGLE_COS) {
                continue;
            }
            double dist = eye.distanceTo(target.position());
            if (dist > CLEAVE_RANGE) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), CLEAVE_DAMAGE);
            totalDamageDealt += CLEAVE_DAMAGE;
        }

        if (totalDamageDealt > 0) {
            player.heal(totalDamageDealt * LIFESTEAL_FRACTION);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.consume(stack);
    }
}
