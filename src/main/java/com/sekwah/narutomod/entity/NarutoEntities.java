package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.item.PaperBombEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenganEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
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
            EntityType.Builder.<FireballJutsuEntity>of(FireballJutsuEntity::new, MobCategory.MISC).sized(1.5F, 1.5F).clientTrackingRange(4).updateInterval(10));

    public static final RegistryObject<EntityType<WaterBulletJutsuEntity>> WATER_BULLET_JUTSU = register("water_bullet_jutsu",
            EntityType.Builder.<WaterBulletJutsuEntity>of(WaterBulletJutsuEntity::new, MobCategory.MISC).fireImmune().sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10));


    public static final RegistryObject<EntityType<SubstitutionLogEntity>> SUBSTITUTION_LOG = register("substitution_log",
            EntityType.Builder.<SubstitutionLogEntity>of(SubstitutionLogEntity::new, MobCategory.MISC).fireImmune().sized(0.3F, 0.3F).clientTrackingRange(4));

    public static final RegistryObject<EntityType<EarthWallEntity>> EARTH_WALL = register("earth_wall",
            EntityType.Builder.<EarthWallEntity>of(EarthWallEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(4));

    public static final RegistryObject<EntityType<ShadowCloneEntity>> SHADOW_CLONE = register("shadow_clone",
            EntityType.Builder.<ShadowCloneEntity>of(ShadowCloneEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(8));

    public static final RegistryObject<EntityType<SummonBeastEntity>> SUMMON_BEAST = register("summon_beast",
            EntityType.Builder.<SummonBeastEntity>of(SummonBeastEntity::new, MobCategory.MISC).sized(2.4F, 2.2F).clientTrackingRange(10));

    public static final RegistryObject<EntityType<RasenganEntity>> RASENGAN = register("rasengan",
            EntityType.Builder.<RasenganEntity>of(RasenganEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(10));

    public static final RegistryObject<EntityType<RasenshurikenEntity>> RASENSHURIKEN = register("rasenshuriken",
            EntityType.Builder.<RasenshurikenEntity>of(RasenshurikenEntity::new, MobCategory.MISC).sized(0.8F, 0.8F).clientTrackingRange(6).updateInterval(10));

    public static final RegistryObject<EntityType<AmaterasuFireEntity>> AMATERASU_FIRE = register("amaterasu_fire",
            EntityType.Builder.<AmaterasuFireEntity>of(AmaterasuFireEntity::new, MobCategory.MISC).fireImmune().sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(10));

    /**
     * Phase 16: an S-rank missing-nin. Spawns through ordinary vanilla mob spawning like
     * any other monster, just at a very rare weight (see the forge biome modifier in data/),
     * so encountering one is a genuine event rather than something on a timer.
     */
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
     * Hashirama's wood golem. MobCategory.MISC because it is conjured by a technique and
     * must never turn up through natural spawning, same as every other summon here.
     */
    public static final RegistryObject<EntityType<WoodGolemEntity>> WOOD_GOLEM = register("wood_golem",
            EntityType.Builder.<WoodGolemEntity>of(WoodGolemEntity::new, MobCategory.MISC)
                    .sized(1.8F, 4.5F).clientTrackingRange(12));

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String key, EntityType.Builder<T> builder) {
        return ENTITIES.register(key, () -> builder.build(key));
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
        event.register(ROGUE_NINJA.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
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
                (type, level, spawnType, pos, random) ->
                        com.sekwah.narutomod.config.NarutoConfig.mangekyoBossSpawnEnabled
                                && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                                && isNearPlayerElevation(level, pos)
                                && net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level, pos, random)
                                && net.minecraft.world.entity.Mob.checkMobSpawnRules(
                                        type, level, spawnType, pos, random),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    /** How far above or below the player a boss may appear. */
    private static final int BOSS_MAX_Y_OFFSET = 20;

    /**
     * Keeps a boss on roughly the player's own layer of the world.
     *
     * Natural spawning is happy to place one in a cave at Y 23 while the player is on the
     * surface at Y 80. The sighting announcement then points at somewhere unreachable, and
     * the boss despawns unfound long before anyone digs down to it. Restricting the vertical
     * offset makes the announcement mean something.
     *
     * Only the bosses get this. Rogue ninja are the zombie substitute and are supposed to
     * fill caves, so narrowing their spawn band would gut their spawn rate.
     */
    private static boolean isNearPlayerElevation(net.minecraft.world.level.ServerLevelAccessor level,
                                                 net.minecraft.core.BlockPos pos) {
        net.minecraft.world.entity.player.Player nearest = level.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, -1.0D, false);
        return nearest != null && Math.abs(nearest.getY() - pos.getY()) <= BOSS_MAX_Y_OFFSET;
    }

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SUBSTITUTION_LOG.get(), SubstitutionLogEntity.createAttributes().build());
        event.put(SHADOW_CLONE.get(), ShadowCloneEntity.createAttributes().build());
        event.put(SUMMON_BEAST.get(), SummonBeastEntity.createAttributes().build());
        event.put(MANGEKYO_BOSS.get(), MangekyoBossEntity.createAttributes().build());
        event.put(ROGUE_NINJA.get(), RogueNinjaEntity.createAttributes().build());
        event.put(WOOD_GOLEM.get(), WoodGolemEntity.createAttributes().build());
    }

}
