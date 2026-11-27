package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.events.BossEncounterEvents;
import com.sekwah.narutomod.entity.item.PaperBombEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenganEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.KirinStrikeEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GreatFireballEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GianBeamEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.IronSandEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.IceSpearEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.ClayBombEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.ShockArcEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterShotEntity;
import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoEntities {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<KunaiEntity>> KUNAI = register("kunai",
            EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<SenbonEntity>> SENBON = register("senbon",
            EntityType.Builder.<SenbonEntity>of(SenbonEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<ExplosiveKunaiEntity>> EXPLOSIVE_KUNAI = register("explosive_kunai",
            EntityType.Builder.<ExplosiveKunaiEntity>of(ExplosiveKunaiEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<HiraishinKunaiEntity>> HIRAISHIN_KUNAI = register("hiraishin_kunai",
            EntityType.Builder.<HiraishinKunaiEntity>of(HiraishinKunaiEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<ShurikenEntity>> SHURIKEN = register("shuriken",
            EntityType.Builder.<ShurikenEntity>of(ShurikenEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<FumaShurikenEntity>> FUMA_SHURIKEN = register("fuma_shuriken",
            EntityType.Builder.<FumaShurikenEntity>of(FumaShurikenEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).setTrackingRange(8));

    // func_233608_b_ is updateInterval
    public static final RegistryObject<EntityType<PaperBombEntity>> PAPER_BOMB = register("paper_bomb",
            EntityType.Builder.<PaperBombEntity>of(PaperBombEntity::new, MobCategory.MISC).fireImmune().sized(0.5F, 0.5F).setTrackingRange(10).clientTrackingRange(10));


    public static final RegistryObject<EntityType<FireballJutsuEntity>> FIREBALL_JUTSU = register("fireball_jutsu",
            EntityType.Builder.<FireballJutsuEntity>of(FireballJutsuEntity::new, MobCategory.MISC).sized(0.42F, 0.42F)
                    .noSave().fireImmune().clientTrackingRange(8).updateInterval(1));

    public static final RegistryObject<EntityType<WaterBulletJutsuEntity>> WATER_BULLET_JUTSU = register("water_bullet_jutsu",
            EntityType.Builder.<WaterBulletJutsuEntity>of(WaterBulletJutsuEntity::new, MobCategory.MISC).fireImmune().sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10));

    public static final RegistryObject<EntityType<WaterShotEntity>> WATER_SHOT = register("water_shot",
            EntityType.Builder.<WaterShotEntity>of(WaterShotEntity::new, MobCategory.MISC).fireImmune().sized(0.36F, 0.36F)
                    .clientTrackingRange(8).updateInterval(1));
    public static final RegistryObject<EntityType<IronSandEntity>> IRON_SAND = register("iron_sand",
            EntityType.Builder.<IronSandEntity>of(IronSandEntity::new, MobCategory.MISC).fireImmune().sized(0.72F, 0.72F)
                    .clientTrackingRange(9).updateInterval(1).noSave());
    public static final RegistryObject<EntityType<IceSpearEntity>> ICE_SPEAR = register("ice_spear",
            EntityType.Builder.<IceSpearEntity>of(IceSpearEntity::new, MobCategory.MISC).sized(0.21F, 0.21F)
                    .clientTrackingRange(9).updateInterval(1).noSave());
    public static final RegistryObject<EntityType<ClayBombEntity>> CLAY_CHARGE = register("clay_charge",
            EntityType.Builder.<ClayBombEntity>of(ClayBombEntity::new, MobCategory.MISC).sized(0.47F, 0.47F)
                    .clientTrackingRange(9).updateInterval(1).noSave());


    public static final RegistryObject<EntityType<SubstitutionLogEntity>> SUBSTITUTION_LOG = register("substitution_log",
            EntityType.Builder.<SubstitutionLogEntity>of(SubstitutionLogEntity::new, MobCategory.MISC).fireImmune().sized(0.3F, 0.3F).clientTrackingRange(4));

    public static final RegistryObject<EntityType<EarthWallEntity>> EARTH_WALL = register("earth_wall",
            EntityType.Builder.<EarthWallEntity>of(EarthWallEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(4));

    public static final RegistryObject<EntityType<ShadowCloneEntity>> SHADOW_CLONE = register("shadow_clone",
            EntityType.Builder.<ShadowCloneEntity>of(ShadowCloneEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<ClayCloneEntity>> CLAY_CLONE = register("clay_clone",
            EntityType.Builder.<ClayCloneEntity>of(ClayCloneEntity::new, MobCategory.MISC).sized(0.6F, 1.8F)
                    .clientTrackingRange(9).updateInterval(1).noSave());
    public static final RegistryObject<EntityType<AnimalPathRhinoEntity>> ANIMAL_RHINO = register("animal_rhino",
            EntityType.Builder.<AnimalPathRhinoEntity>of(AnimalPathRhinoEntity::new, MobCategory.MISC)
                    .sized(com.sekwah.narutomod.util.AnimalPathDrive.WIDTH, com.sekwah.narutomod.util.AnimalPathDrive.HEIGHT)
                    .clientTrackingRange(10).updateInterval(1).noSave());

    public static final RegistryObject<EntityType<NinkenEntity>> NINKEN = register("ninken",
            EntityType.Builder.<NinkenEntity>of(NinkenEntity::new, MobCategory.CREATURE).sized(0.68f, 0.86f)
                    .clientTrackingRange(8).updateInterval(2));

    public static final RegistryObject<EntityType<SummonBeastEntity>> SUMMON_BEAST = register("summon_beast",
            // Registered at Gamabunta's size (contract 0). Every contract overrides
            // getDimensions from its synced variant, so this is only the pre-variant default.
            EntityType.Builder.<SummonBeastEntity>of(SummonBeastEntity::new, MobCategory.MISC).sized(4.4F, 5.0F).clientTrackingRange(16));

    public static final RegistryObject<EntityType<RasenganEntity>> RASENGAN = register("rasengan",
            EntityType.Builder.<RasenganEntity>of(RasenganEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(10));

    public static final RegistryObject<EntityType<RasenshurikenEntity>> RASENSHURIKEN = register("rasenshuriken",
            EntityType.Builder.<RasenshurikenEntity>of(RasenshurikenEntity::new, MobCategory.MISC).sized(0.8F, 0.8F)
                    .clientTrackingRange(12).updateInterval(1));

    public static final RegistryObject<EntityType<AmaterasuFireEntity>> AMATERASU_FIRE = register("amaterasu_fire",
            EntityType.Builder.<AmaterasuFireEntity>of(AmaterasuFireEntity::new, MobCategory.MISC).fireImmune().sized(0.6F, 0.6F)
                    .clientTrackingRange(12).updateInterval(1).noSummon());

    public static final RegistryObject<EntityType<MangekyoBossEntity>> MANGEKYO_BOSS = register("mangekyo_boss",
            EntityType.Builder.<MangekyoBossEntity>of(MangekyoBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(12));

    /**
     * Phase 21: rank-and-file missing-nin. Unlike the Mangekyo bosses these DO use vanilla
     * natural spawning (see the forge biome modifier in data/), because the whole point is
     * having ninja to grind instead of zombies.
     */
    public static final RegistryObject<EntityType<RogueNinjaEntity>> ROGUE_NINJA = register("rogue_ninja",
            EntityType.Builder.<RogueNinjaEntity>of(RogueNinjaEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8));

    /**
     * Uchiha missing-nin. Its own type purely so it can carry a spawn weight distinct from
     * the ordinary rogue's - see the biome modifiers in data/.
     */
    public static final RegistryObject<EntityType<UchihaRogueEntity>> UCHIHA_ROGUE = register("uchiha_rogue",
            EntityType.Builder.<UchihaRogueEntity>of(UchihaRogueEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8));

    /** Registered at Shukaku's size; synced variants override the dimensions. */
    public static final RegistryObject<EntityType<TailedBeastEntity>> TAILED_BEAST = register("tailed_beast",
            EntityType.Builder.<TailedBeastEntity>of(TailedBeastEntity::new, MobCategory.MONSTER)
                    .sized(2.4F, 6.6F).clientTrackingRange(24).fireImmune());

    /** Bijudama. Tracked far out: you are supposed to see one coming. */
    public static final RegistryObject<EntityType<com.sekwah.narutomod.entity.jutsuprojectile.TailedBeastBombEntity>>
            TAILED_BEAST_BOMB = register("tailed_beast_bomb",
            EntityType.Builder.<com.sekwah.narutomod.entity.jutsuprojectile.TailedBeastBombEntity>of(
                            com.sekwah.narutomod.entity.jutsuprojectile.TailedBeastBombEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F).clientTrackingRange(20).updateInterval(2));

    public static final RegistryObject<EntityType<com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity>>
            CHIBAKU_TENSEI = register("chibaku_tensei",
            EntityType.Builder.<com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity>of(
                            com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F).clientTrackingRange(24).updateInterval(2));

    public static final RegistryObject<EntityType<com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity>>
            CHAKRA_DRAGON = register("chakra_dragon",
            EntityType.Builder.<com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity>of(
                            com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(32).updateInterval(1)
                    .noSummon());
    public static final RegistryObject<EntityType<KirinStrikeEntity>> KIRIN_STRIKE = register("kirin_strike",
            EntityType.Builder.of(KirinStrikeEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.5f).clientTrackingRange(24).updateInterval(1).noSummon());
    public static final RegistryObject<EntityType<GreatFireballEntity>> GREAT_FIREBALL = register("great_fireball",
            EntityType.Builder.<GreatFireballEntity>of(GreatFireballEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(12).updateInterval(1).noSummon());
    public static final RegistryObject<EntityType<FireWallEntity>> FIRE_WALL = register("fire_wall",
            EntityType.Builder.<FireWallEntity>of(FireWallEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(16).updateInterval(1).noSummon());
    public static final RegistryObject<EntityType<GianBeamEntity>> GIAN_BEAM = register("gian_beam",
            EntityType.Builder.<GianBeamEntity>of(GianBeamEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(12).updateInterval(1).noSave().noSummon());
    public static final RegistryObject<EntityType<GustFrontEntity>> GUST_FRONT = register("gust_front",
            EntityType.Builder.<GustFrontEntity>of(GustFrontEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(12).updateInterval(1).noSave().noSummon());
    public static final RegistryObject<EntityType<ShockArcEntity>> SHOCK_ARC = register("shock_arc",
            EntityType.Builder.<ShockArcEntity>of(ShockArcEntity::new, MobCategory.MISC)
                    .sized(0.24f, 0.22f).clientTrackingRange(8).updateInterval(1).noSave().noSummon());

    /** Susanoo's thrown seal. A billboard, like the original - there is no model to bake. */
    public static final RegistryObject<EntityType<com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity>>
            YASAKA_MAGATAMA = register("yasaka_magatama",
            EntityType.Builder.<com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity>of(
                            com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(48).updateInterval(1)
                    .noSummon());

    public static final RegistryObject<EntityType<com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity>>
            ICE_MIRROR = register("ice_mirror",
            EntityType.Builder.<com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity>of(
                            com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity::new, MobCategory.MISC)
                    .sized(0.8F, 2.5F).clientTrackingRange(12).updateInterval(1));

    /** Puppets are spawned by a puppeteer; their synced variant overrides Hiruko's base dimensions. */
    public static final RegistryObject<EntityType<PuppetEntity>> PUPPET = register("puppet",
            EntityType.Builder.<PuppetEntity>of(PuppetEntity::new, MobCategory.MISC)
                    .sized(1.5F, 2.9F).clientTrackingRange(12));

    /**
     * Hashirama's wood golem. MobCategory.MISC because it is conjured by a technique and
     * must never turn up through natural spawning, same as every other summon here.
     */
    public static final RegistryObject<EntityType<WoodGolemEntity>> WOOD_GOLEM = register("wood_golem",
            EntityType.Builder.<WoodGolemEntity>of(WoodGolemEntity::new, MobCategory.MISC)
                    .sized(1.8F, 4.5F).clientTrackingRange(12).updateInterval(1).noSave());

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String key, EntityType.Builder<T> builder) {
        return ENTITIES.register(key, () -> builder.build(key));
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
        // checkAnyLightMonsterSpawnRules, not checkMonsterSpawnRules: missing-nin are living
        // people, not undead, and nothing about them says "only after dark". Tying them to
        // the vanilla darkness rule meant the whole ninja population vanished at sunrise and
        // the world went back to being ordinary Minecraft for half of every day.
        event.register(ROGUE_NINJA.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NarutoEntities::checkRogueSpawn,
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(UCHIHA_ROGUE.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NarutoEntities::checkRogueSpawn,
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);

        // Bosses now ride the ordinary monster-spawning pipeline too, just at a far rarer
        // weight (see the biome modifier). The config flag stays meaningful by being folded
        // into the spawn predicate rather than gating a custom timer.
        event.register(MANGEKYO_BOSS.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                // Monster::checkMonsterSpawnRules can't be used directly here: the boss extends
                // PathfinderMob (it only implements Enemy), so it doesn't satisfy that method's
                // `? extends Monster` bound. Same three checks, spelled out.
                // No darkness check either, for the same reason as the rogues above plus a
                // practical one: an S-rank appears rarely enough that halving its window to
                // night only made it feel broken.
                (type, level, spawnType, pos, random) ->
                        level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                                && (!BossSpawnProgression.isAmbient(spawnType)
                                    || com.sekwah.narutomod.config.NarutoConfig.mangekyoBossSpawnEnabled
                                       && BossEncounterEvents.canSpawn(level, pos, BossSpawnProgression.FIRST_NINJA_RANK))
                                && net.minecraft.world.entity.Mob.checkMobSpawnRules(
                                        type, level, spawnType, pos, random),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(TAILED_BEAST.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                                && (!BossSpawnProgression.isAmbient(spawnType)
                                    || BossEncounterEvents.canSpawn(level, pos, BossSpawnProgression.FIRST_BEAST_RANK))
                                && hasRoomForTailedBeast(level, pos)
                                && net.minecraft.world.entity.Mob.checkMobSpawnRules(
                                        type, level, spawnType, pos, random),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static boolean checkRogueSpawn(EntityType<? extends net.minecraft.world.entity.monster.Monster> type,
                                           net.minecraft.world.level.ServerLevelAccessor level,
                                           net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos,
                                           net.minecraft.util.RandomSource random) {
        return (!BossSpawnProgression.isAmbient(reason)
                || !com.sekwah.narutomod.world.KamuiDimension.isKamui(level.getLevel()))
                && net.minecraft.world.entity.monster.Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }

    /** Headroom the largest beast needs; anything shorter and it spawns inside the ceiling. */
    private static final int BIJU_HEADROOM = 12;

    /**
     * Vanilla's spawn check only clears the registered hitbox, which here is Shukaku's - the
     * smallest of the eight. Without this, a Gyuki rolled at that spot would appear with four
     * blocks of itself inside a cave roof and immediately suffocate or be shoved out.
     */
    private static boolean hasRoomForTailedBeast(net.minecraft.world.level.ServerLevelAccessor level,
                                                 net.minecraft.core.BlockPos pos) {
        for (int y = 1; y <= BIJU_HEADROOM; y++) {
            if (!level.getBlockState(pos.above(y)).isAir()) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SUBSTITUTION_LOG.get(), SubstitutionLogEntity.createAttributes().build());
        event.put(SHADOW_CLONE.get(), ShadowCloneEntity.createAttributes().build());
        event.put(CLAY_CLONE.get(), ShadowCloneEntity.createAttributes().build());
        event.put(ANIMAL_RHINO.get(), AnimalPathRhinoEntity.createAttributes().build());
        event.put(NINKEN.get(), net.minecraft.world.entity.animal.Wolf.createAttributes().build());
        event.put(SUMMON_BEAST.get(), SummonBeastEntity.createAttributes().build());
        event.put(MANGEKYO_BOSS.get(), MangekyoBossEntity.createAttributes().build());
        event.put(ROGUE_NINJA.get(), RogueNinjaEntity.createAttributes().build());
        event.put(UCHIHA_ROGUE.get(), RogueNinjaEntity.createAttributes().build());
        event.put(WOOD_GOLEM.get(), WoodGolemEntity.createAttributes().build());
        event.put(TAILED_BEAST.get(), TailedBeastEntity.createAttributes().build());
        event.put(PUPPET.get(), PuppetEntity.createAttributes().build());
    }

}
