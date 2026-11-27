package com.sekwah.narutomod.network;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.c2s.*;
import com.sekwah.narutomod.network.s2c.ClientCooldownPacket;
import com.sekwah.narutomod.network.s2c.ClientTestPacket;
import com.sekwah.narutomod.network.s2c.ChidoriImpactPacket;
import com.sekwah.narutomod.network.s2c.ChidoriDashPacket;
import com.sekwah.narutomod.network.s2c.RasenganImpactPacket;
import com.sekwah.narutomod.network.s2c.NagashiPacket;
import com.sekwah.narutomod.network.s2c.LeapFxPacket;
import com.sekwah.narutomod.network.s2c.SubstitutionFxPacket;
import com.sekwah.narutomod.network.s2c.FlickerPacket;
import com.sekwah.narutomod.network.s2c.KamuiStepPacket;
import com.sekwah.narutomod.network.s2c.SharinganGazePacket;
import com.sekwah.narutomod.network.s2c.TsukuyomiPacket;
import com.sekwah.narutomod.network.s2c.CrowFlockPacket;
import com.sekwah.narutomod.network.s2c.GunbaiGuardPacket;
import com.sekwah.narutomod.network.s2c.KotoInfluencePacket;
import com.sekwah.narutomod.network.s2c.KamuiPhasePacket;
import com.sekwah.narutomod.network.s2c.KamuiTransitPacket;
import com.sekwah.narutomod.network.s2c.SpiritProjectionPacket;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket;
import com.sekwah.narutomod.network.s2c.ShadowGripPacket;
import com.sekwah.narutomod.network.s2c.AdamantineChainPacket;
import com.sekwah.narutomod.network.s2c.KaitenPacket;
import com.sekwah.narutomod.network.s2c.TrigramPalmsPacket;
import com.sekwah.narutomod.network.s2c.MedicalPalmPacket;
import com.sekwah.narutomod.network.s2c.ScalpelPacket;
import com.sekwah.narutomod.network.s2c.BaikaPacket;
import com.sekwah.narutomod.network.s2c.BoulderRollPacket;
import com.sekwah.narutomod.network.s2c.ShinranshinPacket;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket;
import com.sekwah.narutomod.network.s2c.GatsugaPacket;
import com.sekwah.narutomod.network.s2c.KikaichuPacket;
import com.sekwah.narutomod.network.s2c.WoodBurialWavePacket;
import com.sekwah.narutomod.network.s2c.WoodArmPacket;
import com.sekwah.narutomod.network.s2c.WoodHousePacket;
import com.sekwah.narutomod.network.s2c.LaserCircusPacket;
import com.sekwah.narutomod.network.s2c.ShinraWavePacket;
import com.sekwah.narutomod.network.s2c.BanshoTetherPacket;
import com.sekwah.narutomod.network.s2c.PretaGuardPacket;
import com.sekwah.narutomod.network.s2c.NarakaScenePacket;
import com.sekwah.narutomod.network.s2c.SummonSealPacket;
import com.sekwah.narutomod.network.s2c.HiraishinPacket;
import com.sekwah.narutomod.network.s2c.ShadowCloneCloudPacket;
import com.sekwah.narutomod.network.s2c.BodyRitePacket;
import com.sekwah.narutomod.network.s2c.KuramaCloakPacket;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.network.s2c.WaterDragonImpactPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

public class PacketHandler {

    private static final int NETWORK_SCHEMA = 79;
    public static final String PROTOCOL_VERSION =
            ModList.get().getModFileById(NarutoMod.MOD_ID).versionString() + ":" + NETWORK_SCHEMA;

    public static final SimpleChannel NARUTO_CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(NarutoMod.MOD_ID, "naruto_data"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    public static void init() {
        // Server to client packs
        NARUTO_CHANNEL.registerMessage(getPacketID(), ClientTestPacket.class, ClientTestPacket::encode, ClientTestPacket::decode, ClientTestPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ClientCooldownPacket.class, ClientCooldownPacket::encode, ClientCooldownPacket::decode, ClientCooldownPacket.Handler::handle);

        // Client to server packets
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerJutsuCastingPacket.class, ServerJutsuCastingPacket::encode, ServerJutsuCastingPacket::decode, ServerJutsuCastingPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerAbilityActivatePacket.class, ServerAbilityActivatePacket::encode, ServerAbilityActivatePacket::decode, ServerAbilityActivatePacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerAbilityChannelPacket.class, ServerAbilityChannelPacket::encode, ServerAbilityChannelPacket::decode, ServerAbilityChannelPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerToggleNinjaPacket.class, ServerToggleNinjaPacket::encode, ServerToggleNinjaPacket::decode, ServerToggleNinjaPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerSelectClanPacket.class, ServerSelectClanPacket::encode, ServerSelectClanPacket::decode, ServerSelectClanPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerWallWalkDetachPacket.class, ServerWallWalkDetachPacket::encode, ServerWallWalkDetachPacket::decode, ServerWallWalkDetachPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerScrollAdjustPacket.class, ServerScrollAdjustPacket::encode, ServerScrollAdjustPacket::decode, ServerScrollAdjustPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), ServerSusanooColorPacket.class, ServerSusanooColorPacket::encode, ServerSusanooColorPacket::decode, ServerSusanooColorPacket.Handler::handle);
        NARUTO_CHANNEL.registerMessage(getPacketID(), RasenganImpactPacket.class, RasenganImpactPacket::encode,
                RasenganImpactPacket::decode, RasenganImpactPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ChidoriImpactPacket.class, ChidoriImpactPacket::encode,
                ChidoriImpactPacket::decode, ChidoriImpactPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), WaterDragonImpactPacket.class, WaterDragonImpactPacket::encode,
                WaterDragonImpactPacket::decode, WaterDragonImpactPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ChidoriDashPacket.class, ChidoriDashPacket::encode,
                ChidoriDashPacket::decode, ChidoriDashPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), NagashiPacket.class, NagashiPacket::encode,
                NagashiPacket::decode, NagashiPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), LeapFxPacket.class, LeapFxPacket::encode,
                LeapFxPacket::decode, LeapFxPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), SubstitutionFxPacket.class, SubstitutionFxPacket::encode,
                SubstitutionFxPacket::decode, SubstitutionFxPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), FlickerPacket.class, FlickerPacket::encode,
                FlickerPacket::decode, FlickerPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KamuiStepPacket.class, KamuiStepPacket::encode,
                KamuiStepPacket::decode, KamuiStepPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), SharinganGazePacket.class, SharinganGazePacket::encode,
                SharinganGazePacket::decode, SharinganGazePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), TsukuyomiPacket.class, TsukuyomiPacket::encode,
                TsukuyomiPacket::decode, TsukuyomiPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), CrowFlockPacket.class, CrowFlockPacket::encode,
                CrowFlockPacket::decode, CrowFlockPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), GunbaiGuardPacket.class, GunbaiGuardPacket::encode,
                GunbaiGuardPacket::decode, GunbaiGuardPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KotoInfluencePacket.class, KotoInfluencePacket::encode,
                KotoInfluencePacket::decode, KotoInfluencePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KamuiPhasePacket.class, KamuiPhasePacket::encode,
                KamuiPhasePacket::decode, KamuiPhasePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KamuiTransitPacket.class, KamuiTransitPacket::encode,
                KamuiTransitPacket::decode, KamuiTransitPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), SpiritProjectionPacket.class, SpiritProjectionPacket::encode,
                SpiritProjectionPacket::decode, SpiritProjectionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), SpiritProjectionInputPacket.class, SpiritProjectionInputPacket::encode,
                SpiritProjectionInputPacket::decode, SpiritProjectionInputPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ShadowTetherPacket.class, ShadowTetherPacket::encode,
                ShadowTetherPacket::decode, ShadowTetherPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ShadowGripPacket.class, ShadowGripPacket::encode,
                ShadowGripPacket::decode, ShadowGripPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), AdamantineChainPacket.class, AdamantineChainPacket::encode,
                AdamantineChainPacket::decode, AdamantineChainPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KaitenPacket.class, KaitenPacket::encode,
                KaitenPacket::decode, KaitenPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), TrigramPalmsPacket.class, TrigramPalmsPacket::encode,
                TrigramPalmsPacket::decode, TrigramPalmsPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), MedicalPalmPacket.class, MedicalPalmPacket::encode,
                MedicalPalmPacket::decode, MedicalPalmPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ScalpelPacket.class, ScalpelPacket::encode,
                ScalpelPacket::decode, ScalpelPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), BaikaPacket.class, BaikaPacket::encode,
                BaikaPacket::decode, BaikaPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), BoulderRollPacket.class, BoulderRollPacket::encode,
                BoulderRollPacket::decode, BoulderRollPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ShinranshinPacket.class, ShinranshinPacket::encode,
                ShinranshinPacket::decode, ShinranshinPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), NinkenCuePacket.class, NinkenCuePacket::encode,
                NinkenCuePacket::decode, NinkenCuePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), GatsugaPacket.class, GatsugaPacket::encode,
                GatsugaPacket::decode, GatsugaPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KikaichuPacket.class, KikaichuPacket::encode,
                KikaichuPacket::decode, KikaichuPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), WoodBurialWavePacket.class, WoodBurialWavePacket::encode,
                WoodBurialWavePacket::decode, WoodBurialWavePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), WoodArmPacket.class, WoodArmPacket::encode,
                WoodArmPacket::decode, WoodArmPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), WoodHousePacket.class, WoodHousePacket::encode,
                WoodHousePacket::decode, WoodHousePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerLateVisualPackets();
    }

    private static void registerLateVisualPackets() {
        NARUTO_CHANNEL.registerMessage(getPacketID(), LaserCircusPacket.class, LaserCircusPacket::encode,
                LaserCircusPacket::decode, LaserCircusPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ShinraWavePacket.class, ShinraWavePacket::encode,
                ShinraWavePacket::decode, ShinraWavePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), BanshoTetherPacket.class, BanshoTetherPacket::encode,
                BanshoTetherPacket::decode, BanshoTetherPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), PretaGuardPacket.class, PretaGuardPacket::encode,
                PretaGuardPacket::decode, PretaGuardPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), NarakaScenePacket.class, NarakaScenePacket::encode,
                NarakaScenePacket::decode, NarakaScenePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), SummonSealPacket.class, SummonSealPacket::encode,
                SummonSealPacket::decode, SummonSealPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), HiraishinPacket.class, HiraishinPacket::encode,
                HiraishinPacket::decode, HiraishinPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), ShadowCloneCloudPacket.class, ShadowCloneCloudPacket::encode,
                ShadowCloneCloudPacket::decode, ShadowCloneCloudPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), BodyRitePacket.class, BodyRitePacket::encode,
                BodyRitePacket::decode, BodyRitePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), KuramaCloakPacket.class, KuramaCloakPacket::encode,
                KuramaCloakPacket::decode, KuramaCloakPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NARUTO_CHANNEL.registerMessage(getPacketID(), FinalChakraPacket.class, FinalChakraPacket::encode,
                FinalChakraPacket::decode, FinalChakraPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static int packetId = 0;
    /**
     * When called grabs a new packet id and increments.
     *
     * Only really usable because we force the mods to have to be the same version via PROTOCOL_VERSION
     * @return
     */
    private static int getPacketID() {
        return packetId++;
    }


    public static void sendToServer(Object obj) {
        NARUTO_CHANNEL.sendToServer(obj);
    }

    // These are for handling from server side to players.
    public static void sendToPlayer(Object obj, ServerPlayer player) {
        NARUTO_CHANNEL.sendTo(obj, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sentToTrackingAndSelf(Object obj, ServerPlayer player) {
        NARUTO_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), obj);
    }

    public static void sentToTracking(Object obj, Entity entity) {
        NARUTO_CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), obj);
    }

    public static void sendNear(Object packet, ServerLevel level, Vec3 origin, double radius) {
        NARUTO_CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(origin.x, origin.y, origin.z, radius, level.dimension())), packet);
    }

}
