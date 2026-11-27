package com.sekwah.narutomod.world;

import com.mojang.serialization.Codec;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class NinjaPopulation {
    private NinjaPopulation() {
    }

    public static void register(IEventBus bus) {
        var modifiers = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, NarutoMod.MOD_ID);
        modifiers.register("ninja_population", () -> NinjaBiomes.CODEC);
        modifiers.register(bus);
    }

    public static boolean blocks(boolean allowOthers, String namespace, boolean hostile) {
        return !allowOthers && hostile && !NarutoMod.MOD_ID.equals(namespace);
    }

    private static boolean blocks(EntityType<?> type, boolean hostile) {
        return blocks(NarutoConfig.allowOtherHostiles, EntityType.getKey(type).getNamespace(),
                hostile || type.getCategory() == MobCategory.MONSTER);
    }

    private static boolean blocks(Entity entity) {
        return !entity.level().isClientSide() && blocks(entity.getType(), entity instanceof Enemy);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void checkPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (blocks(event.getEntityType(), false)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() && blocks(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void checkDimensionTravel(EntityTravelToDimensionEvent event) {
        if (blocks(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static final class NinjaBiomes implements BiomeModifier {
        private static final Codec<NinjaBiomes> CODEC = Codec.unit(new NinjaBiomes());

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase != Phase.AFTER_EVERYTHING || NarutoConfig.allowOtherHostiles) {
                return;
            }
            var spawns = builder.getMobSpawnSettings();
            for (MobCategory category : MobCategory.values()) {
                spawns.getSpawner(category).removeIf(entry -> blocks(entry.type, false));
            }
            var hostile = spawns.getSpawner(MobCategory.MONSTER);
            if (hostile.stream().noneMatch(entry -> entry.type == NarutoEntities.ROGUE_NINJA.get())) {
                hostile.add(new MobSpawnSettings.SpawnerData(NarutoEntities.ROGUE_NINJA.get(), 135, 1, 3));
            }
            if (hostile.stream().noneMatch(entry -> entry.type == NarutoEntities.UCHIHA_ROGUE.get())) {
                hostile.add(new MobSpawnSettings.SpawnerData(NarutoEntities.UCHIHA_ROGUE.get(), 47, 1, 2));
            }
        }

        @Override
        public Codec<? extends BiomeModifier> codec() {
            return CODEC;
        }
    }
}
