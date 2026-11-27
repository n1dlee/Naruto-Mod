package com.sekwah.narutomod.util;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

public final class ChakraFlow {
    public static final int COAT_TICKS = 4;
    public static final float HIT_COST = 3.0f;
    private static final Edge SWORD = new Edge(pixel(4.5, 4.5, 8), pixel(13.5, 13.5, 8), pixel(0, 0, 0.59), 0.45 / 16);
    private static final Edge KUNAI = new Edge(pixel(8, 8, 10.37), pixel(8, 8, -2.31), pixel(1.05, 0, 0), 0.45 / 16);
    private static final Set<String> VANILLA_SWORDS = Set.of("wooden_sword", "stone_sword", "iron_sword",
            "golden_sword", "diamond_sword", "netherite_sword");
    private static final Map<String, Edge> NINJA_EDGES = Map.of(
            "katana", new Edge(pixel(5.54, 8, 8.2), pixel(15.15, 8, 8), pixel(0, 0.14, 0), 0.15 / 16),
            "chakra_blade", new Edge(pixel(8.1, 6.4, 8.05), pixel(8.9, 10.9, 8.05), pixel(0, 0, 0.34), 0.35 / 16),
            "kusanagi", new Edge(pixel(8, 8, 11.1), pixel(7.98, 8, 0), pixel(0, 0.36, 0), 0.28 / 16),
            "kubikiribocho", new Edge(pixel(7.45, 8.02, 4.5), pixel(8.7, 8.02, 15.92), pixel(0, 0.07, 0), 0.28 / 16),
            "hiraishin_kunai", new Edge(pixel(8, 8, 8), pixel(8, -0.15, 8), pixel(0, 0, 0.46), 0.3 / 16),
            "kunai", KUNAI,
            "explosive_kunai", KUNAI);

    private ChakraFlow() {
    }

    public static boolean active(INinjaData data) {
        return data.isNinjaModeEnabled()
                && data.getToggleAbilityData().getAbilitiesHashSet().contains(NarutoAbilities.CHAKRA_FLOW.getId());
    }

    public static Edge edge(ItemStack stack) {
        return stack.isEmpty() ? null : edge(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static Edge edge(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        if (NarutoMod.MOD_ID.equals(id.getNamespace())) {
            return NINJA_EDGES.get(id.getPath());
        }
        return "minecraft".equals(id.getNamespace()) && VANILLA_SWORDS.contains(id.getPath()) ? SWORD : null;
    }

    public static float fill(float ticks) {
        float t = Mth.clamp(ticks / COAT_TICKS, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static float swing(float progress) {
        return progress > 0 && progress < 1 ? Mth.sin(progress * Mth.PI) : 0;
    }

    public static float bonus(float rankMultiplier) {
        return 5.0f * Mth.clamp(rankMultiplier, 0, 2.5f);
    }

    private static Vec3 pixel(double x, double y, double z) {
        return new Vec3(x, y, z).scale(1.0 / 16.0);
    }

    public record Edge(Vec3 base, Vec3 tip, Vec3 offset, double width) {
    }
}
