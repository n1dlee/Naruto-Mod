package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.ByakuganSight;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ByakuganEvents {
    private ByakuganEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void guard(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            if (data.isNinjaModeEnabled() && data.isByakuganActive() && data.getByakuganRange() > 0) {
                event.setAmount(event.getAmount() * ByakuganSight.shielding(data.getByakuganLevel()));
            }
        });
    }
}
