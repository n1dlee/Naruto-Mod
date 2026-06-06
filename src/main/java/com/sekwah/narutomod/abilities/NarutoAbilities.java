package com.sekwah.narutomod.abilities;

import com.mojang.logging.LogUtils;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.AdamantineChainsAbility;
import com.sekwah.narutomod.abilities.jutsus.BodyFlickerAbility;
import com.sekwah.narutomod.abilities.jutsus.EightTrigramsRotationAbility;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.FlyingThunderGodAbility;
import com.sekwah.narutomod.abilities.jutsus.KuramaCloakAbility;
import com.sekwah.narutomod.abilities.jutsus.MagnetReleaseAbility;
import com.sekwah.narutomod.abilities.jutsus.SageModeAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganGenjutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.WoodReleaseAbility;
import com.sekwah.narutomod.abilities.jutsus.AirPalmAbility;
import com.sekwah.narutomod.abilities.jutsus.AmaterasuAbility;
import com.sekwah.narutomod.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriDashAbility;
import com.sekwah.narutomod.abilities.jutsus.DojutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.EarthSpikesAbility;
import com.sekwah.narutomod.abilities.jutsus.EarthWallJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.FalseDarknessAbility;
import com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.GreatBreakthroughAbility;
import com.sekwah.narutomod.abilities.jutsus.MultipleShadowCloneJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowCloneJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.abilities.jutsus.SubstitutionJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.WaterBulletJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.WaterDragonAbility;
import com.sekwah.narutomod.abilities.utility.ChakraChargeAbility;
import com.sekwah.narutomod.abilities.utility.ChakraDashAbility;
import com.sekwah.narutomod.abilities.utility.DoubleJumpAbility;
import com.sekwah.narutomod.abilities.utility.LeapAbility;
import com.sekwah.narutomod.abilities.utility.WaterWalkAbility;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.ServerAbilityActivatePacket;
import com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoAbilities {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Ability> ABILITY = DeferredRegister.create(NarutoRegistries.ABILITY_REGISTRY_LOC, MOD_ID);

    public static final Map<Long, ResourceLocation> COMBO_MAP = new HashMap<>();


    public static final RegistryObject<LeapAbility> LEAP = ABILITY.register("leap", LeapAbility::new);

    public static final RegistryObject<ChakraDashAbility> CHAKRA_DASH = ABILITY.register("chakra_dash", ChakraDashAbility::new);

    public static final RegistryObject<WaterWalkAbility> WATER_WALK = ABILITY.register("water_walk", WaterWalkAbility::new);

    public static final RegistryObject<FireballJutsuAbility> FIREBALL = ABILITY.register("fireball", FireballJutsuAbility::new);

    public static final RegistryObject<WaterBulletJutsuAbility> WATER_BULLET = ABILITY.register("water_bullet", WaterBulletJutsuAbility::new);

    public static final RegistryObject<ChakraChargeAbility> CHAKRA_CHARGE = ABILITY.register("chakra_charge", ChakraChargeAbility::new);

    public static final RegistryObject<DoubleJumpAbility> DOUBLE_JUMP = ABILITY.register("double_jump", DoubleJumpAbility::new);

    public static final RegistryObject<SubstitutionJutsuAbility> SUBSTITUTION = ABILITY.register("substitution", SubstitutionJutsuAbility::new);

    public static final RegistryObject<EarthWallJutsuAbility> EARTH_WALL = ABILITY.register("earth_wall", EarthWallJutsuAbility::new);

    public static final RegistryObject<ShadowCloneJutsuAbility> SHADOW_CLONE = ABILITY.register("shadow_clone", ShadowCloneJutsuAbility::new);

    public static final RegistryObject<MultipleShadowCloneJutsuAbility> MULTIPLE_SHADOW_CLONE = ABILITY.register("multiple_shadow_clone", MultipleShadowCloneJutsuAbility::new);

    public static final RegistryObject<RasenganJutsuAbility> RASENGAN = ABILITY.register("rasengan", RasenganJutsuAbility::new);

    public static final RegistryObject<DojutsuAbility> DOJUTSU = ABILITY.register("dojutsu", DojutsuAbility::new);

    public static final RegistryObject<SharinganAbility> SHARINGAN = ABILITY.register("sharingan", SharinganAbility::new);

    public static final RegistryObject<ByakuganAbility> BYAKUGAN = ABILITY.register("byakugan", ByakuganAbility::new);

    public static final RegistryObject<AmaterasuAbility> AMATERASU = ABILITY.register("amaterasu", AmaterasuAbility::new);

    public static final RegistryObject<ChidoriAbility> CHIDORI = ABILITY.register("chidori", ChidoriAbility::new);

    public static final RegistryObject<ChidoriDashAbility> CHIDORI_DASH = ABILITY.register("chidori_dash", ChidoriDashAbility::new);

    // --- Phase C: New clan jutsu ---
    public static final RegistryObject<ShadowPossessionAbility> SHADOW_POSSESSION = ABILITY.register("shadow_possession", ShadowPossessionAbility::new);
    public static final RegistryObject<AirPalmAbility> AIR_PALM = ABILITY.register("air_palm", AirPalmAbility::new);
    public static final RegistryObject<AdamantineChainsAbility> ADAMANTINE_CHAINS = ABILITY.register("adamantine_chains", AdamantineChainsAbility::new);
    public static final RegistryObject<EarthSpikesAbility> EARTH_SPIKES = ABILITY.register("earth_spikes", EarthSpikesAbility::new);
    public static final RegistryObject<FalseDarknessAbility> FALSE_DARKNESS = ABILITY.register("false_darkness", FalseDarknessAbility::new);
    public static final RegistryObject<GreatBreakthroughAbility> GREAT_BREAKTHROUGH = ABILITY.register("great_breakthrough", GreatBreakthroughAbility::new);
    public static final RegistryObject<WaterDragonAbility> WATER_DRAGON = ABILITY.register("water_dragon", WaterDragonAbility::new);
    public static final RegistryObject<EightTrigramsRotationAbility> EIGHT_TRIGRAMS_ROTATION = ABILITY.register("eight_trigrams_rotation", EightTrigramsRotationAbility::new);
    public static final RegistryObject<BodyFlickerAbility> BODY_FLICKER = ABILITY.register("body_flicker", BodyFlickerAbility::new);
    public static final RegistryObject<SharinganGenjutsuAbility> SHARINGAN_GENJUTSU = ABILITY.register("sharingan_genjutsu", SharinganGenjutsuAbility::new);
    public static final RegistryObject<SageModeAbility> SAGE_MODE = ABILITY.register("sage_mode", SageModeAbility::new);

    // --- Phase 4: Advanced Jutsu ---
    public static final RegistryObject<FlyingThunderGodAbility> FLYING_THUNDER_GOD = ABILITY.register("flying_thunder_god", FlyingThunderGodAbility::new);
    public static final RegistryObject<EightGatesAbility> EIGHT_GATES = ABILITY.register("eight_gates", EightGatesAbility::new);
    public static final RegistryObject<WoodReleaseAbility> WOOD_RELEASE = ABILITY.register("wood_release", WoodReleaseAbility::new);
    public static final RegistryObject<MagnetReleaseAbility> MAGNET_RELEASE = ABILITY.register("magnet_release", MagnetReleaseAbility::new);

    // --- Phase 5: Jinchuriki ---
    public static final RegistryObject<KuramaCloakAbility> KURAMA_CLOAK = ABILITY.register("kurama_cloak", KuramaCloakAbility::new);

    public static void register(IEventBus eventBus) {
        ABILITY.register(eventBus);
    }

    /**
     * May change how key combos are handled in the future but these will be default
     */
    public static void registerKeyCombos() {
        ABILITY.getEntries().forEach(abilityEntry -> {
            Ability ability = abilityEntry.get();
            long combo = ability.defaultCombo();
            if (combo > 0) {
                if(COMBO_MAP.containsKey(combo)) {
                    LOGGER.error("Ability already registered with that combo {}", combo);
                } else {
                    NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(resourceKey -> COMBO_MAP.put(combo, resourceKey.location()));
                }
            }
        });
    }

    /**
     * Send to the server that the player wants to use a specific ability
     */
    public static void triggerAbility(ResourceLocation ability) {
        PacketHandler.sendToServer(new ServerAbilityActivatePacket(ability));
    }

    public static Ability getAbilityFromCombo(long combo) {
        if(COMBO_MAP.containsKey(combo)) {
            return NarutoRegistries.ABILITIES.getValue(COMBO_MAP.get(combo));
        } else {
            return null;
        }
    }

    public static boolean handleCharging(long combo, ServerAbilityChannelPacket.ChannelStatus channelStatus) {
        if(COMBO_MAP.containsKey(combo)) {
            ResourceLocation abilityResource = COMBO_MAP.get(combo);
            PacketHandler.sendToServer(new ServerAbilityChannelPacket(abilityResource, channelStatus));
            return true;
        } else {
            return false;
        }
    }

    public static boolean triggerAbility(long combo) {
        if(COMBO_MAP.containsKey(combo)) {
            triggerAbility(COMBO_MAP.get(combo));
            return true;
        } else {
            return false;
        }
    }


    @SubscribeEvent
    public static void clientSetup(FMLCommonSetupEvent event) {
        registerKeyCombos();
    }
}
