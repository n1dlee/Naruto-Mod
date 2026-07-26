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
     * Phase 16: the first genuinely hostile mob in the mod — every other entity here is
     * summoned by an ability, so this is the only MONSTER-category registration. Spawning
     * is handled deliberately by MangekyoBossSpawner rather than vanilla spawn rules.
     */
    public static final RegistryObject<EntityType<MangekyoBossEntity>> MANGEKYO_BOSS = register("mangekyo_boss",
            EntityType.Builder.<MangekyoBossEntity>of(MangekyoBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(12));

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String key, EntityType.Builder<T> builder) {
        return ENTITIES.register(key, () -> builder.build(key));
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SUBSTITUTION_LOG.get(), SubstitutionLogEntity.createAttributes().build());
        event.put(SHADOW_CLONE.get(), ShadowCloneEntity.createAttributes().build());
        event.put(SUMMON_BEAST.get(), SummonBeastEntity.createAttributes().build());
        event.put(MANGEKYO_BOSS.get(), MangekyoBossEntity.createAttributes().build());
    }

}
