package com.sekwah.narutomod.network.c2s;

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

    private final int abilityId;

    public ServerAbilityActivatePacket(ResourceLocation ability) {
        this.abilityId = NarutoRegistries.ABILITIES.getID(ability);
    }

    public ServerAbilityActivatePacket(int abilityId) {
        this.abilityId = abilityId;
    }

    public static void encode(ServerAbilityActivatePacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.abilityId);
    }

    public static ServerAbilityActivatePacket decode(FriendlyByteBuf inBuffer) {
        int abilityId = inBuffer.readInt();
        return new ServerAbilityActivatePacket(abilityId);
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
                    Ability ability = NarutoRegistries.ABILITIES.getValue(msg.abilityId);
                    // Phase 15: scroll-taught jutsu must be learned; elemental jutsu need their
                    // element unlocked + trained. Phase 16 adds the dojutsu gate.
                    if (!ability.checkLearnedRequirement(player, ninjaData)
                            || !ability.checkElementRequirement(player, ninjaData)
                            || !ability.checkEyeRequirement(player, ninjaData)) {
                        if (ability.castingFailSound() != null) {
                            player.playNotifySound(ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                        return;
                    }
                    if (ability.activationType() == Ability.ActivationType.INSTANT) {

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
                            ninjaData.setCastPoseTicks(8);

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
