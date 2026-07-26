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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kabutowari — the "bluntsword", an axe and hammer joined by a leather rope. Canon says
 * it breaks through any defence, so its heavy smash deals magic damage (armour and
 * enchantments do not soften it), shatters an active shield outright, and leaves the
 * victim's guard broken for a moment.
 */
public class KabutowariItem extends SwordItem {

    private static final double SMASH_RANGE = 4.5;
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(55));
    private static final float SMASH_DAMAGE = 14.0f;
    private static final int GUARD_BREAK_TICKS = 4 * 20;
    private static final int SHIELD_DISABLE_TICKS = 100;
    private static final int COOLDOWN_TICKS = 150;

    public KabutowariItem(Properties properties) {
        super(NinjaTier.KATANA, 6, -3.0f, properties);
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
        boolean hitSomething = false;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(SMASH_RANGE)).inflate(SMASH_RANGE * 0.5),
                e -> e != player && e.isAlive())) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            if (toTarget.dot(look) < HALF_ANGLE_COS || eye.distanceTo(target.position()) > SMASH_RANGE) {
                continue;
            }
            // Magic damage: "breaks through any defence" means armour does not apply
            target.hurt(player.damageSources().magic(), SMASH_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GUARD_BREAK_TICKS, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, GUARD_BREAK_TICKS, 1, false, true));
            // A raised shield is simply smashed aside
            if (target instanceof Player defender && defender.isUsingItem() && defender.isBlocking()) {
                defender.getCooldowns().addCooldown(defender.getUseItem().getItem(), SHIELD_DISABLE_TICKS);
                defender.stopUsingItem();
            }
            hitSomething = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 impact = player.position().add(look.x * 2.0, 0.2, look.z * 2.0);
            NarutoParticles.spawnRing(serverLevel, impact, 2.0, 24, NarutoParticles.METAL_GRAY);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    impact.x, impact.y + 0.5, impact.z, 20, 0.5, 0.3, 0.5, 0.05);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.8f);

        player.getCooldowns().addCooldown(this, hitSomething ? COOLDOWN_TICKS : COOLDOWN_TICKS / 3);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.kabutowari.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
