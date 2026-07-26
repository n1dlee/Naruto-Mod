package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.item.NinjaTier;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kiba — the "thunderswords", the sharpest blades in existence, permanently charged with
 * lightning. Every hit arcs to a second nearby enemy; RMB discharges the stored lightning
 * as a short chain through everything close by. Owning the Lightning nature makes the
 * arcs bite harder, which is the canon detail that these blades amplify a lightning user.
 */
public class KibaBladesItem extends SwordItem {

    private static final float ARC_DAMAGE = 5.0f;
    private static final double ARC_RANGE = 5.0;
    private static final float DISCHARGE_DAMAGE = 9.0f;
    private static final double DISCHARGE_RADIUS = 6.0;
    private static final int MAX_CHAIN = 4;
    private static final int COOLDOWN_TICKS = 120;
    private static final float LIGHTNING_AFFINITY_BONUS = 1.5f;

    public KibaBladesItem(Properties properties) {
        super(NinjaTier.KATANA, 4, -1.8f, properties);
    }

    /** Lightning-natured wielders push more current through the blades. */
    private static float affinityMultiplier(Player player) {
        return player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve()
                .filter(data -> data.isElementUnlocked("lightning"))
                .map(data -> LIGHTNING_AFFINITY_BONUS)
                .orElse(1.0f);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide && attacker instanceof Player player) {
            LivingEntity nearest = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(ARC_RANGE),
                    e -> e != attacker && e != target && e.isAlive())) {
                double distance = candidate.distanceTo(target);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = candidate;
                }
            }
            if (nearest != null) {
                DamageSource source = NarutoDamageTypes.getDamageSource(
                        level, NarutoDamageTypes.CHIDORI, player, player);
                nearest.hurt(source, ARC_DAMAGE * affinityMultiplier(player));
                drawArc(level, target.position().add(0, target.getBbHeight() * 0.5, 0),
                        nearest.position().add(0, nearest.getBbHeight() * 0.5, 0));
            }
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

        DamageSource source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.CHIDORI, player, player);
        float damage = DISCHARGE_DAMAGE * affinityMultiplier(player);

        // Chain from the wielder through the nearest targets, up to MAX_CHAIN links
        Vec3 from = player.position().add(0, 1.0, 0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(DISCHARGE_RADIUS), e -> e != player && e.isAlive());
        nearby.sort(java.util.Comparator.comparingDouble(player::distanceTo));

        int links = 0;
        for (LivingEntity target : nearby) {
            if (links >= MAX_CHAIN) {
                break;
            }
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            target.hurt(source, damage);
            drawArc(level, from, to);
            from = to;
            links++;
        }

        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 0.9f, 1.6f);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return links > 0 ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private static void drawArc(Level level, Vec3 from, Vec3 to) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int steps = Math.max(4, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, i / (double) steps);
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN,
                    point.x, point.y, point.z, 1, 0.06, 0.06, 0.06, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.kiba.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
