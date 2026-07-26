package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Akimichi Clan — Human Boulder / Nikudan Sensha (combo 233, INSTANT).
 * The Akimichi tucks into a ball and rolls straight through the enemy line: a blunt
 * forward charge that flattens and scatters everything along the path. Rolls half
 * again as hard while Baika no Jutsu (giant form) is active.
 */
public class HumanBoulderAbility extends Ability implements Ability.Cooldown {

    private static final ResourceLocation BAIKA_ID = new ResourceLocation("narutomod", "baika");
    private static final float CHAKRA_COST = 35f;
    private static final double CHARGE_DISTANCE = 9.0;
    private static final double HIT_RADIUS = 1.6;
    private static final float DAMAGE = 8.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 233;
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.IRON_GOLEM_ATTACK;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"akimichi".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.akimichi",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        boolean giant = ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(BAIKA_ID);
        float damage = DAMAGE * ninjaData.getRankDamageMultiplier() * (giant ? 1.5f : 1.0f);

        // Flatten everything along the roll path
        Vec3 start = player.position().add(0, 0.5, 0);
        Vec3 path = flat.scale(CHARGE_DISTANCE);
        AABB rollBox = player.getBoundingBox().expandTowards(path).inflate(HIT_RADIUS);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, rollBox,
                e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(start);
            double along = toTarget.dot(flat);
            if (along < 0 || along > CHARGE_DISTANCE) continue;
            if (toTarget.subtract(flat.scale(along)).horizontalDistance() > HIT_RADIUS) continue;
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.knockback(giant ? 2.2 : 1.5, -flat.x, -flat.z);
        }

        // Launch the roll
        player.setDeltaMovement(flat.x * 2.2, 0.15, flat.z * 2.2);
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i <= 12; i++) {
                Vec3 pos = start.add(flat.scale(CHARGE_DISTANCE * i / 12.0));
                serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 2, 0.3, 0.2, 0.3, 0.02);
            }
        }
    }
}
