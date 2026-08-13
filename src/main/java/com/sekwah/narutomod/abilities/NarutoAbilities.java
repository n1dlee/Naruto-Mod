package com.sekwah.narutomod.abilities;

import com.mojang.logging.LogUtils;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.AdamantineChainsAbility;
import com.sekwah.narutomod.abilities.jutsus.AnimalPathAbility;
import com.sekwah.narutomod.abilities.jutsus.ChakraFlowAbility;
import com.sekwah.narutomod.abilities.jutsus.HiraishinTeleportAbility;
import com.sekwah.narutomod.abilities.jutsus.LightningArmorAbility;
import com.sekwah.narutomod.abilities.jutsus.LightningShockAbility;
import com.sekwah.narutomod.abilities.jutsus.BanshoTeninAbility;
import com.sekwah.narutomod.abilities.jutsus.CrowGenjutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.NarakaPathAbility;
import com.sekwah.narutomod.abilities.jutsus.PretaPathAbility;
import com.sekwah.narutomod.abilities.jutsus.ShinraTenseiAbility;
import com.sekwah.narutomod.abilities.jutsus.GunbaiWindAbility;
import com.sekwah.narutomod.abilities.jutsus.KamuiPhaseAbility;
import com.sekwah.narutomod.abilities.jutsus.KirinAbility;
import com.sekwah.narutomod.abilities.jutsus.KotoamatsukamiAbility;
import com.sekwah.narutomod.abilities.jutsus.TsukuyomiAbility;
import com.sekwah.narutomod.abilities.jutsus.BodyFlickerAbility;
import com.sekwah.narutomod.abilities.jutsus.EightTrigramsRotationAbility;
import com.sekwah.narutomod.abilities.jutsus.EightTrigramsSixtyFourPalmsAbility;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.FlyingThunderGodAbility;
import com.sekwah.narutomod.abilities.jutsus.KuramaChakraModeAbility;
import com.sekwah.narutomod.abilities.jutsus.KuramaCloakAbility;
import com.sekwah.narutomod.abilities.jutsus.MagnetReleaseAbility;
import com.sekwah.narutomod.abilities.jutsus.SageModeAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganGenjutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.SusanooAbility;
import com.sekwah.narutomod.abilities.jutsus.WoodReleaseAbility;
import com.sekwah.narutomod.abilities.jutsus.AirPalmAbility;
import com.sekwah.narutomod.abilities.jutsus.AmaterasuAbility;
import com.sekwah.narutomod.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriDashAbility;
import com.sekwah.narutomod.abilities.jutsus.BaikaAbility;
import com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility;
import com.sekwah.narutomod.abilities.jutsus.GatsugaAbility;
import com.sekwah.narutomod.abilities.jutsus.HumanBoulderAbility;
import com.sekwah.narutomod.abilities.jutsus.KikaichuSwarmAbility;
import com.sekwah.narutomod.abilities.jutsus.KuchiyoseAbility;
import com.sekwah.narutomod.abilities.jutsus.MindDisturbanceAbility;
import com.sekwah.narutomod.abilities.jutsus.NinkenAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriNagashiAbility;
import com.sekwah.narutomod.abilities.jutsus.KamuiAbility;
import com.sekwah.narutomod.abilities.jutsus.MysticalPalmAbility;
import com.sekwah.narutomod.abilities.jutsus.DojutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.EarthSpikesAbility;
import com.sekwah.narutomod.abilities.jutsus.EarthWallJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.FalseDarknessAbility;
import com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.GreatBreakthroughAbility;
import com.sekwah.narutomod.abilities.jutsus.MultipleShadowCloneJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenshurikenAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowCloneJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowSewingAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowStrangleAbility;
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
    public static final RegistryObject<RasenshurikenAbility> RASENSHURIKEN = ABILITY.register("rasenshuriken", RasenshurikenAbility::new);

    // Fire had exactly one technique for the whole game; these fill the mid and top of it.
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.PhoenixFlowerAbility> PHOENIX_FLOWER =
            ABILITY.register("phoenix_flower",
                    com.sekwah.narutomod.abilities.jutsus.PhoenixFlowerAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.GreatFireAnnihilationAbility> GREAT_FIRE_ANNIHILATION =
            ABILITY.register("great_fire_annihilation",
                    com.sekwah.narutomod.abilities.jutsus.GreatFireAnnihilationAbility::new);

    public static final RegistryObject<DojutsuAbility> DOJUTSU = ABILITY.register("dojutsu", DojutsuAbility::new);

    public static final RegistryObject<SharinganAbility> SHARINGAN = ABILITY.register("sharingan", SharinganAbility::new);

    public static final RegistryObject<ByakuganAbility> BYAKUGAN = ABILITY.register("byakugan", ByakuganAbility::new);

    public static final RegistryObject<AmaterasuAbility> AMATERASU = ABILITY.register("amaterasu", AmaterasuAbility::new);

    public static final RegistryObject<ChidoriAbility> CHIDORI = ABILITY.register("chidori", ChidoriAbility::new);

    public static final RegistryObject<ChidoriDashAbility> CHIDORI_DASH = ABILITY.register("chidori_dash", ChidoriDashAbility::new);
    public static final RegistryObject<ChidoriNagashiAbility> CHIDORI_NAGASHI = ABILITY.register("chidori_nagashi", ChidoriNagashiAbility::new);
    public static final RegistryObject<KamuiAbility> KAMUI = ABILITY.register("kamui", KamuiAbility::new);

    // --- Phase C: New clan jutsu ---
    public static final RegistryObject<ShadowPossessionAbility> SHADOW_POSSESSION = ABILITY.register("shadow_possession", ShadowPossessionAbility::new);
    public static final RegistryObject<ShadowSewingAbility> SHADOW_SEWING = ABILITY.register("shadow_sewing", ShadowSewingAbility::new);
    public static final RegistryObject<ShadowStrangleAbility> SHADOW_STRANGLE = ABILITY.register("shadow_strangle", ShadowStrangleAbility::new);
    public static final RegistryObject<AirPalmAbility> AIR_PALM = ABILITY.register("air_palm", AirPalmAbility::new);
    public static final RegistryObject<AdamantineChainsAbility> ADAMANTINE_CHAINS = ABILITY.register("adamantine_chains", AdamantineChainsAbility::new);
    public static final RegistryObject<EarthSpikesAbility> EARTH_SPIKES = ABILITY.register("earth_spikes", EarthSpikesAbility::new);
    public static final RegistryObject<FalseDarknessAbility> FALSE_DARKNESS = ABILITY.register("false_darkness", FalseDarknessAbility::new);
    public static final RegistryObject<GreatBreakthroughAbility> GREAT_BREAKTHROUGH = ABILITY.register("great_breakthrough", GreatBreakthroughAbility::new);
    public static final RegistryObject<WaterDragonAbility> WATER_DRAGON = ABILITY.register("water_dragon", WaterDragonAbility::new);
    public static final RegistryObject<EightTrigramsRotationAbility> EIGHT_TRIGRAMS_ROTATION = ABILITY.register("eight_trigrams_rotation", EightTrigramsRotationAbility::new);
    public static final RegistryObject<EightTrigramsSixtyFourPalmsAbility> EIGHT_TRIGRAMS_SIXTY_FOUR_PALMS = ABILITY.register("eight_trigrams_sixty_four_palms", EightTrigramsSixtyFourPalmsAbility::new);
    public static final RegistryObject<BodyFlickerAbility> BODY_FLICKER = ABILITY.register("body_flicker", BodyFlickerAbility::new);
    public static final RegistryObject<SharinganGenjutsuAbility> SHARINGAN_GENJUTSU = ABILITY.register("sharingan_genjutsu", SharinganGenjutsuAbility::new);
    public static final RegistryObject<SageModeAbility> SAGE_MODE = ABILITY.register("sage_mode", SageModeAbility::new);
    public static final RegistryObject<MysticalPalmAbility> MYSTICAL_PALM = ABILITY.register("mystical_palm", MysticalPalmAbility::new);
    public static final RegistryObject<ChakraScalpelAbility> CHAKRA_SCALPEL = ABILITY.register("chakra_scalpel", ChakraScalpelAbility::new);

    // --- Phase 4: Advanced Jutsu ---
    public static final RegistryObject<FlyingThunderGodAbility> FLYING_THUNDER_GOD = ABILITY.register("flying_thunder_god", FlyingThunderGodAbility::new);
    public static final RegistryObject<EightGatesAbility> EIGHT_GATES = ABILITY.register("eight_gates", EightGatesAbility::new);
    public static final RegistryObject<WoodReleaseAbility> WOOD_RELEASE = ABILITY.register("wood_release", WoodReleaseAbility::new);
    public static final RegistryObject<MagnetReleaseAbility> MAGNET_RELEASE = ABILITY.register("magnet_release", MagnetReleaseAbility::new);

    // --- Phase 5: Jinchuriki ---
    public static final RegistryObject<KuramaCloakAbility> KURAMA_CLOAK = ABILITY.register("kurama_cloak", KuramaCloakAbility::new);

    // --- Phase 7: Mangekyo Sharingan / Susanoo ---
    public static final RegistryObject<SusanooAbility> SUSANOO = ABILITY.register("susanoo", SusanooAbility::new);

    // --- Phase 16: Eternal Mangekyo form signatures (each taken from the boss who owned it) ---
    public static final RegistryObject<TsukuyomiAbility> TSUKUYOMI = ABILITY.register("tsukuyomi", TsukuyomiAbility::new);
    public static final RegistryObject<CrowGenjutsuAbility> CROW_GENJUTSU = ABILITY.register("crow_genjutsu", CrowGenjutsuAbility::new);
    public static final RegistryObject<KirinAbility> KIRIN = ABILITY.register("kirin", KirinAbility::new);
    public static final RegistryObject<GunbaiWindAbility> GUNBAI_WIND = ABILITY.register("gunbai_wind", GunbaiWindAbility::new);
    public static final RegistryObject<KotoamatsukamiAbility> KOTOAMATSUKAMI = ABILITY.register("kotoamatsukami", KotoamatsukamiAbility::new);
    public static final RegistryObject<KamuiPhaseAbility> KAMUI_PHASE = ABILITY.register("kamui_phase", KamuiPhaseAbility::new);

    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.KamuiWarpAbility> KAMUI_WARP =
            ABILITY.register("kamui_warp", com.sekwah.narutomod.abilities.jutsus.KamuiWarpAbility::new);

    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.ByakuganScoutAbility> BYAKUGAN_SCOUT =
            ABILITY.register("byakugan_scout", com.sekwah.narutomod.abilities.jutsus.ByakuganScoutAbility::new);

    // --- Phase 17: Senju / Mokuton (Wood Release). The cage lives on as WOOD_RELEASE,
    // which already existed and was already named "Wood Prison" - it was rewritten in
    // place rather than duplicated. ---
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.WoodBurialAbility> WOOD_BURIAL =
            ABILITY.register("wood_burial", com.sekwah.narutomod.abilities.jutsus.WoodBurialAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.WoodArmAbility> WOOD_ARM =
            ABILITY.register("wood_arm", com.sekwah.narutomod.abilities.jutsus.WoodArmAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.WoodHouseAbility> WOOD_HOUSE =
            ABILITY.register("wood_house", com.sekwah.narutomod.abilities.jutsus.WoodHouseAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.WoodGolemAbility> WOOD_GOLEM =
            ABILITY.register("wood_golem", com.sekwah.narutomod.abilities.jutsus.WoodGolemAbility::new);

    // --- Phase 17: kekkei genkai. No clan lock on these three - unlike Mokuton they are
    // combination natures anyone who trains both parents can reach (see Ability.secondaryElement). ---
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.IceSpikesAbility> ICE_SPIKES =
            ABILITY.register("ice_spikes", com.sekwah.narutomod.abilities.jutsus.IceSpikesAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.IceSpearBarrageAbility> ICE_SPEAR_BARRAGE =
            ABILITY.register("ice_spear_barrage", com.sekwah.narutomod.abilities.jutsus.IceSpearBarrageAbility::new);
    /** Phase 22: the mirrors themselves, as a ring of entities you can break. */
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.IceMirrorsAbility> ICE_MIRRORS =
            ABILITY.register("ice_mirrors", com.sekwah.narutomod.abilities.jutsus.IceMirrorsAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.StormAuraAbility> STORM_AURA =
            ABILITY.register("storm_aura", com.sekwah.narutomod.abilities.jutsus.StormAuraAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.ClayBombAbility> CLAY_BOMB =
            ABILITY.register("clay_bomb", com.sekwah.narutomod.abilities.jutsus.ClayBombAbility::new);
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.ExplosiveCloneAbility> EXPLOSIVE_CLONE =
            ABILITY.register("explosive_clone", com.sekwah.narutomod.abilities.jutsus.ExplosiveCloneAbility::new);

    // --- Phase 16: Rinnegan — the Six Paths ---
    public static final RegistryObject<ShinraTenseiAbility> SHINRA_TENSEI = ABILITY.register("shinra_tensei", ShinraTenseiAbility::new);
    public static final RegistryObject<BanshoTeninAbility> BANSHO_TENIN = ABILITY.register("bansho_tenin", BanshoTeninAbility::new);
    /** Phase 22: the Deva Path's last word, as a core that hangs and pulls. */
    public static final RegistryObject<com.sekwah.narutomod.abilities.jutsus.ChibakuTenseiAbility> CHIBAKU_TENSEI =
            ABILITY.register("chibaku_tensei", com.sekwah.narutomod.abilities.jutsus.ChibakuTenseiAbility::new);
    public static final RegistryObject<PretaPathAbility> PRETA_PATH = ABILITY.register("preta_path", PretaPathAbility::new);
    public static final RegistryObject<NarakaPathAbility> NARAKA_PATH = ABILITY.register("naraka_path", NarakaPathAbility::new);
    public static final RegistryObject<AnimalPathAbility> ANIMAL_PATH = ABILITY.register("animal_path", AnimalPathAbility::new);

    // --- Phase 20: entry-level lightning, chakra flow, Raikage armour, Hiraishin jump ---
    public static final RegistryObject<LightningShockAbility> LIGHTNING_SHOCK = ABILITY.register("lightning_shock", LightningShockAbility::new);
    public static final RegistryObject<ChakraFlowAbility> CHAKRA_FLOW = ABILITY.register("chakra_flow", ChakraFlowAbility::new);
    public static final RegistryObject<LightningArmorAbility> LIGHTNING_ARMOR = ABILITY.register("lightning_armor", LightningArmorAbility::new);
    public static final RegistryObject<HiraishinTeleportAbility> HIRAISHIN_TELEPORT = ABILITY.register("hiraishin_teleport", HiraishinTeleportAbility::new);

    // --- Phase 8: Kurama Chakra Mode ---
    public static final RegistryObject<KuramaChakraModeAbility> KCM = ABILITY.register("kurama_chakra_mode", KuramaChakraModeAbility::new);

    // --- Phase 13: New clans (Akimichi / Yamanaka / Inuzuka / Aburame) ---
    public static final RegistryObject<BaikaAbility> BAIKA = ABILITY.register("baika", BaikaAbility::new);
    public static final RegistryObject<HumanBoulderAbility> HUMAN_BOULDER = ABILITY.register("human_boulder", HumanBoulderAbility::new);
    public static final RegistryObject<MindDisturbanceAbility> MIND_DISTURBANCE = ABILITY.register("mind_disturbance", MindDisturbanceAbility::new);
    public static final RegistryObject<NinkenAbility> NINKEN = ABILITY.register("ninken", NinkenAbility::new);
    public static final RegistryObject<GatsugaAbility> GATSUGA = ABILITY.register("gatsuga", GatsugaAbility::new);
    public static final RegistryObject<KikaichuSwarmAbility> KIKAICHU_SWARM = ABILITY.register("kikaichu_swarm", KikaichuSwarmAbility::new);
    public static final RegistryObject<KuchiyoseAbility> KUCHIYOSE = ABILITY.register("kuchiyose", KuchiyoseAbility::new);

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
        // Only ever called from the client's combo handler, so the local player's live WASD
        // state is readable here and nowhere on the server - it rides along with the cast so
        // directional techniques can use it. See the packet's own note.
        float[] input = net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.sekwah.narutomod.client.ClientInputAccess::currentMoveInput);
        if (input == null) {
            input = new float[]{0f, 0f};
        }
        PacketHandler.sendToServer(ServerAbilityActivatePacket.withInput(
                NarutoRegistries.ABILITIES.getID(ability), input[0], input[1]));
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
