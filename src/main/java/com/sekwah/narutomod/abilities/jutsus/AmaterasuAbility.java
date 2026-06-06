package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AmaterasuAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 70.0F;
    private static final double RANGE = 32.0D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 113;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"uchiha".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uchiha",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getSharinganLevel() < 4) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.kage",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 target = findTarget(player);
        AmaterasuFireEntity fire = new AmaterasuFireEntity(player.level(), player, target.x, target.y, target.z);
        fire.setDamageMultiplier(ninjaData.getRankDamageMultiplier());
        player.level().addFreshEntity(fire);
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    private Vec3 findTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        HitResult blockHit = player.pick(RANGE, 0.0F, false);
        Vec3 blockTarget = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        double blockDistance = eye.distanceToSqr(blockTarget);

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, searchBox,
                entity -> canTarget(player, entity));
        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= blockDistance) {
            return entityHit.getEntity().position().add(0.0D, entityHit.getEntity().getBbHeight() * 0.5D, 0.0D);
        }
        return blockTarget;
    }

    private boolean canTarget(Player player, Entity entity) {
        return entity != player && !entity.isSpectator() && entity.isPickable();
    }
}
