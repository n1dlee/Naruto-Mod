package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.weapons.GunbaiItem;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.GunbaiGuardPacket;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.GunbaiMotion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunbaiGuardEvents {
    private GunbaiGuardEvents() {
    }

    @SubscribeEvent
    public static void poseOnly(ShieldBlockEvent event) {
        if (event.getEntity().getUseItem().getItem() instanceof GunbaiItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void reflect(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player)
                || !player.isUsingItem() || player.isSpectator() || !player.isAlive()
                || !(player.getUseItem().getItem() instanceof GunbaiItem)
                || !(event.getProjectile() instanceof AbstractArrow arrow) || !supported(arrow)
                || !enemy(player, arrow.getOwner())) {
            return;
        }
        var level = player.serverLevel();
        Vec3 normal = player.getLookAngle();
        Vec3 contact = GunbaiMotion.intercept(GunbaiMotion.guardCenter(player, player.getUsedItemHand()),
                normal, arrow.position(), hit.getLocation());
        if (contact == null || level.clip(new ClipContext(arrow.position(), contact, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
            return;
        }
        event.setCanceled(true);
        bounce(arrow, player, contact, normal);
        player.stopUsingItem();
        level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.94f, 0.75f);
        PacketHandler.sendNear(new GunbaiGuardPacket(level.dimension().location(), contact, normal), level, contact, 36);
    }

    private static boolean supported(AbstractArrow arrow) {
        return arrow.getClass() == Arrow.class || arrow.getClass() == SpectralArrow.class;
    }

    private static boolean enemy(ServerPlayer defender, Entity source) {
        return source == null || !defender.isAlliedTo(source) && !Faction.sameSide(defender, source)
                && (!(source instanceof Player player) || defender.canHarmPlayer(player));
    }

    private static void bounce(AbstractArrow arrow, ServerPlayer defender, Vec3 contact, Vec3 normal) {
        Vec3 rebound = GunbaiMotion.bounce(arrow.getDeltaMovement(), normal);
        Vec3 point = contact.add(normal.scale(0.08));
        arrow.setPos(point.x, point.y, point.z);
        arrow.setOwner(defender);
        arrow.setDeltaMovement(rebound);
        arrow.setYRot((float) Math.toDegrees(Math.atan2(rebound.x, rebound.z)));
        arrow.setXRot((float) Math.toDegrees(Math.atan2(rebound.y, rebound.horizontalDistance())));
        arrow.yRotO = arrow.getYRot();
        arrow.xRotO = arrow.getXRot();
        arrow.hasImpulse = true;
        arrow.hurtMarked = true;
    }
}
