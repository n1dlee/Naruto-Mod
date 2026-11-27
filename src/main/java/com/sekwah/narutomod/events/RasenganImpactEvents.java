package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.RasenganImpactPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RasenganImpactEvents {
    private static final float BLOCK_IMPACT_COST = 15.0F;
    private static final double WIND_KNOCKBACK_PER_LEVEL = 0.9;
    private static final int WIND_LEVEL_CAP = 10;
    private static final double EFFECT_RANGE = 48.0;

    private RasenganImpactEvents() {
    }

    static void onMeleeHit(LivingHurtEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || source.getDirectEntity() != attacker || !source.is(DamageTypes.PLAYER_ATTACK)
                || event.getAmount() <= 0.0F) {
            return;
        }
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data ->
                strikeEntity(attacker, event.getEntity(), data));
    }

    private static void strikeEntity(ServerPlayer attacker, LivingEntity target, INinjaData data) {
        float strength = strengthForCharge(data.getRasenganCharge());
        if (!data.isNinjaModeEnabled() || !data.consumeHeldRasengan()) {
            return;
        }

        float damage = (target instanceof Player ? 14.0F : 15.0F + strength * 25.0F)
                * data.getRankDamageMultiplier();
        if (target instanceof Player && data.getNinjaRank() < 4) {
            damage = Math.min(damage, target.getHealth() - 1.0F);
        }
        if (damage <= 0.0F) {
            return;
        }

        Vec3 eye = attacker.getEyePosition();
        Vec3 aim = attacker.getLookAngle();
        Vec3 contact = contactOnTarget(target.getBoundingBox(), eye, aim, attacker.getEntityReach());
        Vec3 direction = contact.subtract(eye).normalize();
        var source = NarutoDamageTypes.getDamageSource(attacker.level(), NarutoDamageTypes.RASENGAN, attacker, attacker);
        if (target.hurt(source, damage)) {
            launchTarget(attacker, target, data, strength);
            broadcast(attacker.serverLevel(), contact,
                    direction.lengthSqr() == 0 ? aim : direction, strength, -1);
        }
    }

    static Vec3 contactOnTarget(AABB box, Vec3 eye, Vec3 aim, double reach) {
        Vec3 center = box.getCenter();
        return box.clip(eye, eye.add(aim.scale(reach)))
                .or(() -> box.clip(eye, center))
                .orElse(center);
    }

    private static void launchTarget(Player attacker, LivingEntity target, INinjaData data, float strength) {
        Vec3 offset = target.position().subtract(attacker.position());
        double horizontal = offset.horizontalDistance();
        if (horizontal <= 0.001) {
            return;
        }
        double wind = Mth.clamp(data.getElementLevel("wind"), 0, WIND_LEVEL_CAP) * WIND_KNOCKBACK_PER_LEVEL;
        target.knockback(4.0 + strength * 6.0 + wind, -offset.x / horizontal, -offset.z / horizontal);
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, Math.min(motion.y + 0.6 + wind * 0.08, 1.6), motion.z);
        target.hurtMarked = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) {
            return;
        }
        BlockPos pos = event.getPos();
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> strikeBlock(player, pos, data));
    }

    private static void strikeBlock(ServerPlayer player, BlockPos pos, INinjaData data) {
        if (!data.isNinjaModeEnabled() || !data.isRasenganHeld() || data.getChakra() < BLOCK_IMPACT_COST) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) {
            return;
        }
        if (!(player.pick(player.getBlockReach(), 1.0F, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) {
            return;
        }
        int charge = data.getRasenganCharge();
        if (!data.consumeHeldRasengan()) {
            return;
        }
        data.useChakra(BLOCK_IMPACT_COST, 10);
        int blockStateId = Block.getId(level.getBlockState(pos));
        if (ForgeEventFactory.getMobGriefingEvent(level, player)) {
            breakCrater(player, level, pos, charge >= 45 ? 2 : 1);
        }
        broadcast(level, hit.getLocation(), Vec3.atLowerCornerOf(hit.getDirection().getNormal()),
                strengthForCharge(charge), blockStateId);
    }

    private static void breakCrater(ServerPlayer player, ServerLevel level, BlockPos center, int radius) {
        var gameType = player.gameMode.getGameModeForPlayer();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                    || player.blockActionRestricted(level, pos, gameType)) {
                continue;
            }
            var state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }
            if (ForgeHooks.onBlockBreakEvent(level, gameType, player, pos) >= 0 && level.getBlockState(pos) == state) {
                level.destroyBlock(pos, true, player);
            }
        }
    }

    private static float strengthForCharge(int charge) {
        return Mth.clamp((charge - 20) / 40.0F, 0.0F, 1.0F);
    }

    private static void broadcast(ServerLevel level, Vec3 contact, Vec3 direction, float strength, int blockStateId) {
        var packet = new RasenganImpactPacket(level.dimension().location(), contact, direction, strength,
                blockStateId, level.random.nextInt());
        PacketHandler.sendNear(packet, level, contact, EFFECT_RANGE);
        var sound = packet.hitsBlock() ? NarutoSounds.RASENGAN_HIT_BLOCK : NarutoSounds.RASENGAN_HIT_ENTITY;
        level.playSound(null, contact.x, contact.y, contact.z, sound.get(),
                packet.hitsBlock() ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.15F, 1.08F - strength * 0.14F);
    }
}
