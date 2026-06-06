package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NinjaTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Kusanagi Sword — Uchiha-only legendary blade.
 * RMB: extends a chakra blade 5 blocks forward, dealing 12 damage to the first entity hit.
 * Costs 30 chakra. Cooldown: 5 seconds.
 */
public class KusanagiSwordItem extends SwordItem {

    private static final float CHAKRA_COST = 30f;
    private static final float EXTEND_DAMAGE = 12f;
    private static final double REACH = 5.0;

    public KusanagiSwordItem(Properties properties) {
        super(NinjaTier.KATANA, 4, -2.0f, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // Uchiha clan check
        var ref = new Object() { boolean isUchiha = false; boolean hasChakra = false; };
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            ref.isUchiha = "uchiha".equals(ninjaData.getClanId());
            if (ref.isUchiha && ninjaData.getChakra() >= CHAKRA_COST) {
                ninjaData.useChakra(CHAKRA_COST, 15);
                ref.hasChakra = true;
            }
        });

        if (!ref.isUchiha) {
            player.displayClientMessage(
                    Component.translatable("jutsu.fail.uchiha",
                            Component.literal("Kusanagi").withStyle(ChatFormatting.YELLOW)), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!ref.hasChakra) {
            player.displayClientMessage(Component.literal("Not enough chakra!").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // Raycast: check entities along the look direction
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH));

        // Broad AABB search
        AABB searchBox = new AABB(eye, end).inflate(1.0);
        List<Entity> entities = level.getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive());

        LivingEntity closest = null;
        double closestDist = REACH + 1;

        for (Entity e : entities) {
            // Check if entity is roughly along the line
            Vec3 toEntity = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double dot = toEntity.dot(look);
            if (dot > 0 && dot < REACH) {
                Vec3 proj = eye.add(look.scale(dot));
                double dist = proj.distanceTo(e.position().add(0, e.getBbHeight() / 2, 0));
                if (dist < 1.5 && dot < closestDist) {
                    closestDist = dot;
                    closest = (LivingEntity) e;
                }
            }
        }

        // White blade particles along the line
        if (level instanceof ServerLevel serverLevel) {
            for (double d = 0.5; d < REACH; d += 0.3) {
                Vec3 p = eye.add(look.scale(d));
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(0.95f, 0.95f, 1.0f), 0.8f),
                        p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }

        if (closest != null) {
            closest.hurt(player.damageSources().playerAttack(player), EXTEND_DAMAGE);
            level.playSound(null, closest.getX(), closest.getY(), closest.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.getCooldowns().addCooldown(this, 5 * 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Always enchanted glow
    }
}
