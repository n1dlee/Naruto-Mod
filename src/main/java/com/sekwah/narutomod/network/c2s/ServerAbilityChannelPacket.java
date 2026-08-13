package com.sekwah.narutomod.network.c2s;

import com.mojang.logging.LogUtils;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityChannelPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation abilityResource;
    private final ChannelStatus status;

    public ServerAbilityChannelPacket(ResourceLocation abilityResource, ChannelStatus status) {
        this.abilityResource = abilityResource;
        this.status = status;
    }

    public enum ChannelStatus {
        START,
        STOP,
        MIN_ACTIVATE
    }

    public static void encode(ServerAbilityChannelPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeResourceLocation(msg.abilityResource);
        outBuffer.writeInt(msg.status.ordinal());
    }

    public static ServerAbilityChannelPacket decode(FriendlyByteBuf inBuffer) {
        ResourceLocation abilityResource = inBuffer.readResourceLocation();
        int status = inBuffer.readInt();
        // Never index the enum with a number off the wire. A modified client sending any
        // other int threw ArrayIndexOutOfBounds inside the decoder, which takes down the
        // whole connection rather than dropping one bad packet.
        ChannelStatus[] values = ChannelStatus.values();
        ChannelStatus resolved = status >= 0 && status < values.length ? values[status] : null;
        if (resolved == null) {
            LOGGER.warn("Discarding channel packet for {} with out-of-range status {}",
                    abilityResource, status);
            resolved = ChannelStatus.STOP;
        }
        return new ServerAbilityChannelPacket(abilityResource, resolved);
    }

    public static class Handler {
        public static void handle(ServerAbilityChannelPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                final ServerPlayer player = ctx.get().getSender();
                if(player == null || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return;
                }
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    if(!ninjaData.isNinjaModeEnabled()) {
                        player.displayClientMessage(Component.translatable("jutsu.not_a_ninja").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    Ability ability = NarutoRegistries.ABILITIES.getValue(msg.abilityResource);
                    if(ability == null) {
                        LOGGER.error("Ability doesnt exist {}", msg.abilityResource);
                        return;
                    }
                    // Phase 15: scroll-taught jutsu must be learned; elemental jutsu need their
                    // element unlocked + trained. Gating START also covers STOP.
                    // Phase 16 adds the dojutsu gate.
                    if (!ability.checkLearnedRequirement(player, ninjaData)
                            || !ability.checkClanRequirement(player, ninjaData)
                            || !ability.checkElementRequirement(player, ninjaData)
                            || !ability.checkEyeRequirement(player, ninjaData)) {
                        if (ability.castingFailSound() != null) {
                            player.playNotifySound(ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                        return;
                    }
                    // Just check if its
                    if (ability.activationType() == Ability.ActivationType.CHANNELED) {
                        if (msg.status == ChannelStatus.START) {
                            // Cooldowns used to be checked only on the INSTANT path, so a
                            // channeled ability that declared one silently never had it.
                            // Gate the START here, where refusing costs the player nothing.
                            if (ability instanceof Ability.Cooldown cooldownAbility
                                    && cooldownAbility.checkCooldown(player, ninjaData,
                                            ability.getTranslationKey(ninjaData))) {
                                return;
                            }
                            ninjaData.setCurrentlyChanneledAbility(player, ability);
                        } else if (msg.status == ChannelStatus.STOP) {
                            // A STOP with nothing being channelled is what a modified client
                            // sends to walk straight into this branch; the field is null then,
                            // and the equals below was dereferencing it.
                            ResourceLocation channelled = ninjaData.getCurrentlyChanneledAbility();
                            if (channelled == null) {
                                return;
                            }
                            NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(resourceKey -> {
                                if(channelled.equals(resourceKey.location())) {
                                    int channelledTicks = ninjaData.getCurrentlyChanneledTicks();
                                    ability.performServer(player, ninjaData, channelledTicks);
                                    ability.grantCastXp(ninjaData);
                                    // Channeled techniques were invisible to the copy wheel:
                                    // only the instant-cast path ever offered itself up, so
                                    // a watching Sharingan could never read one.
                                    com.sekwah.narutomod.util.SharinganCopy.onJutsuPerformed(
                                            player, ability, resourceKey.location().getPath());
                                    if (ability instanceof Ability.Cooldown cooldownAbility
                                            && ability.channelCommittedAt(channelledTicks)) {
                                        cooldownAbility.registerCooldown(ninjaData,
                                                ability.getTranslationKey(ninjaData));
                                    }
                                    ninjaData.setCurrentlyChanneledAbility(player, null);
                                }
                            });
                        } else if(msg.status == ChannelStatus.MIN_ACTIVATE) {
                            if (ability instanceof Ability.Channeled channeled && channeled.canActivateBelowMinCharge()) {
                                /*
                                 * The tap-to-cast path had no cooldown at either end: it never
                                 * refused a cast that was still on cooldown, and it never put
                                 * one on afterwards. Every channeled technique that allows a
                                 * tap - the Great Fireball above all - was therefore free to
                                 * spam at whatever the tap happened to cost, which for the
                                 * fireball was a single charging tick.
                                 */
                                if (ability instanceof Ability.Cooldown cooldownAbility
                                        && cooldownAbility.checkCooldown(player, ninjaData,
                                                ability.getTranslationKey(ninjaData))) {
                                    return;
                                }
                                if(ability.handleCost(player, ninjaData, -1)) {
                                    if (ability.castingSound() != null) {
                                        player.level().playSound(null, player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);

                                        player.level().gameEvent(player, NarutoGameEvents.JUTSU_CASTING.get(), player.position().add(0, player.getEyeHeight() * 0.7, 0));
                                    }
                                    player.displayClientMessage(Component.translatable("jutsu.cast", Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                                    ability.performServer(player, ninjaData, -1);
                                    ability.grantCastXp(ninjaData);
                                    if (ability instanceof Ability.Cooldown cooldownAbility) {
                                        cooldownAbility.registerCooldown(ninjaData,
                                                ability.getTranslationKey(ninjaData));
                                    }
                                    // A tapped cast is still a cast, so a watching Sharingan
                                    // gets to read it - the same as the charged path does.
                                    NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(key ->
                                            com.sekwah.narutomod.util.SharinganCopy.onJutsuPerformed(
                                                    player, ability, key.location().getPath()));
                                }
                            } else {
                                player.displayClientMessage(Component.translatable("jutsu.channel.needed", Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
                            }
                        }
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
