package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NinjaCapabilityHandler {

    public static final Capability<INinjaData> NINJA_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(NarutoMod.MOD_ID, "ninja_data"), new NinjaData(!event.getObject().level().isClientSide));
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if(event.phase.equals(TickEvent.Phase.END)) {
            Player player = event.player;
            player.getCapability(NINJA_DATA).ifPresent(data -> {
                if(event.side.isServer()) {
                    if(player.isCreative()) {
                        data.setChakra(data.getMaxChakra());
                        data.setStamina(data.getMaxStamina());
                    }
                    data.updateDataServer(player);
                    if(!player.isSpectator()) {
                        new ArrayList<>(data.getToggleAbilityData().getAbilitiesHashSet()).forEach(abilityName -> {
                            Ability ability = NarutoRegistries.ABILITIES.getValue(abilityName);
                            if(ability != null && ability.handleCost(player, data)) {
                                ability.performServer(player, data);
                            } else if (ability != null) {
                                data.getToggleAbilityData().removeAbilityEnded(player, data, ability);
                            }
                        });
                    }
                } else {
                    data.updateDataClient(player);
                    if(!player.isSpectator()) {
                        new ArrayList<>(data.getToggleAbilityData().getAbilitiesHashSet()).forEach(abilityName -> {
                            Ability ability = NarutoRegistries.ABILITIES.getValue(abilityName);
                            if(ability instanceof Ability.Toggled toggleAbility) toggleAbility.performToggleClient(player, data);
                        });
                    }
                }
            });
        }

    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(NINJA_DATA).ifPresent(original -> {
            event.getEntity().getCapability(NINJA_DATA).ifPresent(future -> {
                future.deserializeNBT(original.serializeNBT());
            });
        });
    }

    //public static final EntitySize STANDING_SIZE = EntitySize.flexible(0.1F, 0.1F);

    /**
     * Makes a final form take up the space it appears to.
     *
     * Until now the giant was only ever drawn. The hitbox stayed 0.6 by 1.8, so an
     * eighteen-block Susanoo could be punched in the ankles by anything that walked up to it,
     * could not block a corridor, fitted through a doorway, and read exactly as the complaint
     * had it - the wielder standing on the ground with a picture around them.
     *
     * Runs on BOTH sides, unlike the client-only eye-height override in RenderEvents: the
     * hitbox has to agree with the server or hits land nowhere near where they look.
     *
     * The width is kept well under the height on purpose. A box as wide as the form is tall
     * would wedge in any terrain narrower than a plain, and a giant that cannot move is worse
     * than one that is slightly too easy to hit.
     */
    @SubscribeEvent
    public static void playerSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isGiantForm()) {
                return;
            }
            float height = com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS;
            event.setNewSize(net.minecraft.world.entity.EntityDimensions.scalable(
                    height * GIANT_WIDTH_RATIO, height));
            event.setNewEyeHeight(height * 0.85f);
        });
    }

    /** Footprint as a fraction of height. A third keeps the giant mobile in real terrain. */
    private static final float GIANT_WIDTH_RATIO = 0.33f;
}
