package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NinjaTier;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Uchiha Gunbai — Madara's iron war fan. One sweep throws out a wall of wind that hurls
 * enemies back and turns any incoming projectile around to fly at whoever threw it.
 *
 * A wielder carrying Madara's Mangekyo form swings it as he did — the blast lands harder
 * and reaches further, tying the weapon into the dojutsu progression instead of sitting
 * beside it.
 */
public class GunbaiItem extends SwordItem {

    private static final double BASE_RANGE = 10.0;
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(50));
    private static final float BASE_DAMAGE = 8.0f;
    private static final double KNOCKBACK = 2.0;
    private static final float MADARA_BONUS = 1.6f;
    private static final int COOLDOWN_TICKS = 100;

    public GunbaiItem(Properties properties) {
        super(NinjaTier.KATANA, 6, -2.8f, properties);
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

        float power = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve()
                .filter(data -> data.hasSignatureForm("madara"))
                .map(data -> MADARA_BONUS)
                .orElse(1.0f);
        double range = BASE_RANGE * power;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(range)).inflate(range * 0.5),
                e -> e != player && e.isAlive())) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            if (toTarget.normalize().dot(look) < HALF_ANGLE_COS || toTarget.length() > range) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), BASE_DAMAGE * power);
            Vec3 push = look.scale(KNOCKBACK * power).add(0, 0.45, 0);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;
        }

        // The fan's real trick: anything flying at you is sent straight back
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().expandTowards(look.scale(range * 0.5)).inflate(4.0))) {
            if (projectile.getOwner() == player) {
                continue;
            }
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.4));
            projectile.setOwner(player);
            projectile.hasImpulse = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            for (int step = 1; step <= (int) range; step++) {
                Vec3 point = eye.add(look.scale(step));
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        point.x, point.y, point.z, 5, step * 0.1, step * 0.1, step * 0.1, 0.02);
            }
            NarutoParticles.spawnRing(serverLevel, eye.add(look.scale(1.5)), 1.6, 24, ParticleTypes.CLOUD);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 1.1f, 0.8f);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.gunbai.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
