package com.sekwah.narutomod.network.c2s;

import com.mojang.logging.LogUtils;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
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

import java.util.HashSet;
import java.util.function.Supplier;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityActivatePacket {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final int abilityId;
    /**
     * The caster's WASD state at the moment they pressed the combo, quantised to -1/0/1.
     *
     * It has to travel with the cast: a ServerPlayer's own xxa/zza are only filled in while
     * riding something, so the server has no other way to know which way the player was
     * holding. Directional techniques - Leap being the first - read it back off NinjaData.
     */
    private final byte strafeInput;
    private final byte forwardInput;

    public ServerAbilityActivatePacket(ResourceLocation ability) {
        this(NarutoRegistries.ABILITIES.getID(ability), (byte) 0, (byte) 0);
    }

    public ServerAbilityActivatePacket(int abilityId) {
        this(abilityId, (byte) 0, (byte) 0);
    }

    public ServerAbilityActivatePacket(int abilityId, byte strafeInput, byte forwardInput) {
        this.abilityId = abilityId;
        this.strafeInput = strafeInput;
        this.forwardInput = forwardInput;
    }

    /** Reads the local player's movement keys, for the client side of a cast. */
    public static ServerAbilityActivatePacket withInput(int abilityId, float strafe, float forward) {
        return new ServerAbilityActivatePacket(abilityId, quantise(strafe), quantise(forward));
    }

    private static byte quantise(float axis) {
        if (axis > 0.1f) {
            return 1;
        }
        return axis < -0.1f ? (byte) -1 : (byte) 0;
    }

    public static void encode(ServerAbilityActivatePacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.abilityId);
        outBuffer.writeByte(msg.strafeInput);
        outBuffer.writeByte(msg.forwardInput);
    }

    public static ServerAbilityActivatePacket decode(FriendlyByteBuf inBuffer) {
        int abilityId = inBuffer.readInt();
        return new ServerAbilityActivatePacket(abilityId, inBuffer.readByte(), inBuffer.readByte());
    }

    public static class Handler {
        public static void handle(ServerAbilityActivatePacket msg, Supplier<NetworkEvent.Context> ctx) {
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
                    // Hand the caster's WASD state over before any ability logic runs, so a
                    // directional technique can read it inside performServer.
                    ninjaData.setMoveInput(msg.strafeInput, msg.forwardInput);
                    Ability ability = NarutoRegistries.ABILITIES.getValue(msg.abilityId);
                    // An id that is not in the registry comes back null, and every check below
                    // dereferences it. A modified client only had to send one bad int.
                    if (ability == null) {
                        LOGGER.warn("Discarding activate packet from {} for unknown ability id {}",
                                player.getGameProfile().getName(), msg.abilityId);
                        return;
                    }
                    // Phase 15: scroll-taught jutsu must be learned; elemental jutsu need their
                    // element unlocked + trained. Phase 16 adds the dojutsu gate.
                    if (!ability.checkLearnedRequirement(player, ninjaData)
                            || !ability.checkClanRequirement(player, ninjaData)
                            || !ability.checkElementRequirement(player, ninjaData)
                            || !ability.checkEyeRequirement(player, ninjaData)) {
                        if (ability.castingFailSound() != null) {
                            player.playNotifySound(ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                        return;
                    }
                    if (ability.activationType() == Ability.ActivationType.INSTANT) {
                        // Gated here rather than in the prelude above: the prelude also runs
                        // for the toggle branch, where it would refuse the packet that turns
                        // a Rasengan OFF, since a held Rasengan is exactly what it reports as
                        // blocking. That left no way to put the sphere away.
                        if (!ability.checkFreeHands(player, ninjaData)) {
                            if (ability.castingFailSound() != null) {
                                player.playNotifySound(ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                            }
                            return;
                        }

                        boolean canTriggerJutsu = true;
                        if (ability  instanceof Ability.Cooldown) {
                            canTriggerJutsu = !((Ability.Cooldown) ability).checkCooldown(player, ninjaData, ability.getTranslationKey(ninjaData));
                        }

                        if(canTriggerJutsu && ability.handleCost(player, ninjaData)) {
                            if (ability.logInChat()) {
                                player.displayClientMessage(Component.translatable("jutsu.cast", Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                            }
                            if(ability.castingSound() != null) {
                                player.level().playSound(null,
                                        player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                                player.level().gameEvent(player, NarutoGameEvents.JUTSU_CASTING.get(), player.position().add(0, player.getEyeHeight() * 0.7, 0));
                            }
                            ninjaData.setCrossSealPose(false);
                            NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(key -> ninjaData.setLastCastAbilityId(key.location()));
                            ability.performServer(player, ninjaData);
                            ability.grantCastXp(ninjaData);
                            strainMangekyo(player, ninjaData, ability);
                            spendCopiedJutsu(ninjaData, ability);
                            broadcastForSharingan(player, ability);
                            ninjaData.setCastPoseTicks(ability.castPoseTicks());

                            if (ability  instanceof Ability.Cooldown cooldownAbility) {
                               cooldownAbility.registerCooldown(ninjaData, ability.getTranslationKey(ninjaData));
                               if (cooldownAbility.getCooldown() > 0) {
                                   com.sekwah.narutomod.network.PacketHandler.sendToPlayer(
                                           new com.sekwah.narutomod.network.s2c.ClientCooldownPacket(
                                                   ability.getTranslationKey(ninjaData), cooldownAbility.getCooldown()),
                                           player);
                               }
                            }
                        } else if(ability.castingFailSound() != null) {
                            player.playNotifySound( ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                    } else if(ability.activationType() == Ability.ActivationType.TOGGLE) {
                        ToggleAbilityData abilityTracker = ninjaData.getToggleAbilityData();
                        HashSet<ResourceLocation> abilities = abilityTracker.getAbilitiesHashSet();
                        NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(abilityResourceKey -> {
                            var location = abilityResourceKey.location();
                            if(abilities.contains(location)) {
                                // Toggle ability off
                                abilityTracker.removeAbilityEnded(player, ninjaData, ability);
                            } else {
                                // Turning one ON is a cast; turning one off, handled above,
                                // never is.
                                if (!ability.checkFreeHands(player, ninjaData)) {
                                    if (ability.castingFailSound() != null) {
                                        player.playNotifySound(ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                                    }
                                    return;
                                }
                                if (ability instanceof Ability.ToggleStartCheck startCheck && !startCheck.canStartToggle(player, ninjaData)) {
                                    if (ability.castingFailSound() != null) {
                                        player.playNotifySound(ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                                    }
                                    return;
                                }
                                // Toggle ability on
                                if (ability.castingSound() != null) {
                                    player.level().playSound(null,
                                            player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                                    player.level().gameEvent(player, NarutoGameEvents.JUTSU_CASTING.get(), player.position().add(0, player.getEyeHeight() * 0.7, 0));
                                }
                                abilityTracker.addAbilityStarted(player, ninjaData, ability);
                                strainMangekyo(player, ninjaData, ability);
                            }
                        });
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }

        /**
         * Phase 16: casting with an ordinary Mangekyo burns the eyes — each use blinds the
         * caster for twice as long as the last. Eternal Mangekyo is immune (see
         * NinjaData#registerMangekyoUse), which is the whole reward for hunting the bosses.
         */
        /**
         * A stolen technique is good for exactly one throw — clear it the moment it is used
         * so the Sharingan has to read a fresh one.
         */
        private static void spendCopiedJutsu(INinjaData ninjaData, Ability ability) {
            if (ability.isCopiedBySharingan(ninjaData)) {
                ninjaData.setCopiedJutsu("");
            }
        }

        /** Lets any Sharingan watching this cast try to read and store it. */
        private static void broadcastForSharingan(ServerPlayer player, Ability ability) {
            NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(key ->
                    com.sekwah.narutomod.util.SharinganCopy.onJutsuPerformed(
                            player, ability, key.location().getPath()));
        }

        private static void strainMangekyo(ServerPlayer player, INinjaData ninjaData, Ability ability) {
            String eye = ability.requiredEye();
            if (eye != null && eye.startsWith("sharingan_ms")) {
                ninjaData.registerMangekyoUse(player);
            }
        }
    }
}
