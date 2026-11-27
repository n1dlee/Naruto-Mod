package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.ClayCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.ClayBombEntity;
import com.sekwah.narutomod.util.JutsuAim;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClayChargeEvents {
    private static final Map<Player, WeakReference<ClayBombEntity>> CHARGES = new WeakHashMap<>();

    private ClayChargeEvents() {
    }

    public static void bind(Player owner, ClayBombEntity charge) { CHARGES.put(owner, new WeakReference<>(charge)); }

    public static boolean detonateExisting(Player owner) {
        ClayBombEntity charge = active(owner);
        if (charge == null) {
            return false;
        }
        if (!charge.remote(owner)) {
            owner.displayClientMessage(Component.translatable("jutsu.clay.not_armed"), true);
        }
        return true;
    }

    public static @Nullable ClayBombEntity active(Player owner) {
        var ref = CHARGES.get(owner);
        ClayBombEntity charge = ref == null ? null : ref.get();
        return charge != null && !charge.isRemoved() && charge.level() == owner.level() && charge.pending() ? charge : null;
    }

    @SubscribeEvent
    public static void filter(ExplosionEvent.Detonate event) {
        var direct = event.getExplosion().getDirectSourceEntity();
        Player owner = direct instanceof ClayBombEntity charge && charge.getOwner() instanceof Player player ? player
                : direct instanceof ClayCloneEntity clone ? clone.caster() : null;
        if (owner != null) {
            event.getAffectedEntities().removeIf(entity -> entity instanceof LivingEntity actor && !JutsuAim.attackable(owner, actor));
            event.getAffectedBlocks().removeIf(pos -> !owner.level().mayInteract(owner, pos));
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player owner) {
            var ref = CHARGES.remove(owner);
            ClayBombEntity charge = ref == null ? null : ref.get();
            if (charge != null && !charge.isRemoved()) {
                charge.fizzle();
            }
        }
    }
}
