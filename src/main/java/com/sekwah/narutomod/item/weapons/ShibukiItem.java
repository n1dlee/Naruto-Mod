package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.item.NinjaTier;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shibuki — the "blastsword". Its scroll feeds explosive tags into the blade, so every
 * swing carries a detonation. Melee hits set off a small blast on the victim; RMB
 * slams the ground for a larger one around the wielder.
 *
 * Block damage follows the same config switch the paper bomb uses, so a player who has
 * turned off terrain damage does not get their base wrecked by their own sword.
 */
public class ShibukiItem extends SwordItem {

    private static final float HIT_BLAST_DAMAGE = 4.0f;
    private static final double HIT_BLAST_RADIUS = 2.0;
    private static final float SLAM_DAMAGE = 12.0f;
    private static final double SLAM_RADIUS = 5.0;
    private static final int COOLDOWN_TICKS = 160;

    public ShibukiItem(Properties properties) {
        super(NinjaTier.KATANA, 5, -2.6f, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide) {
            // A tag goes off against whatever the blade just touched
            for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(HIT_BLAST_RADIUS),
                    e -> e != attacker && e.isAlive())) {
                caught.hurt(level.damageSources().explosion(attacker, attacker), HIT_BLAST_DAMAGE);
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        3, 0.3, 0.3, 0.3, 0.0);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        return super.hurtEnemy(stack, target, attacker);
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

        Vec3 origin = player.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SLAM_RADIUS), e -> e != player && e.isAlive())) {
            double falloff = Math.max(0.0, 1.0 - target.position().distanceTo(origin) / SLAM_RADIUS);
            target.hurt(level.damageSources().explosion(player, player), (float) (SLAM_DAMAGE * falloff));
            Vec3 push = target.position().subtract(origin).normalize().scale(1.2 * falloff);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.4 * falloff, push.z));
            target.hurtMarked = true;
        }

        level.explode(player, origin.x, origin.y, origin.z, 2.0f,
                NarutoConfig.paperbombBlockDamage
                        ? Level.ExplosionInteraction.TNT
                        : Level.ExplosionInteraction.NONE);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.shibuki.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
