package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ByakuganEntityVisionGUI implements PlayerGUI {

    private static final int MAX_TRACKED_ENTITIES = 128;
    /** Beyond a 32-chunk render distance the client has nothing left to report. */
    private static final int MAX_SEARCH_RADIUS = 512;
    private final Minecraft minecraft;
    private final List<LivingEntity> visibleEntities = new ArrayList<>();
    private boolean active;
    private int range;

    public ByakuganEntityVisionGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.active || worldMatrix == null || cameraPos == null) {
            return;
        }

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        for (LivingEntity entity : this.visibleEntities) {
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            double distance = cameraPos.distanceTo(center);
            Vector4f vec = new Vector4f((float) (center.x - cameraPos.x), (float) (center.y - cameraPos.y), (float) (center.z - cameraPos.z), 1.0F);
            vec.mul(worldMatrix);
            if (vec.w <= 0.0F) {
                continue;
            }
            vec.div(vec.w);
            if (vec.z() <= 0.0F || vec.z() >= 1.0F) {
                continue;
            }

            int x = Math.round(halfWidth + vec.x() * halfWidth);
            int y = Math.round(halfHeight - vec.y() * halfHeight);
            int size = Math.max(4, (int) (14.0D / Math.max(1.0D, distance / 20.0D)));
            int alpha = Math.max(60, 150 - (int) Math.min(110, distance / Math.max(1, this.range) * 110));
            int color = (alpha << 24) | 0xBFEFFF;

            /*
             * Four corner ticks, and nothing in the middle.
             *
             * This used to draw a full box with a crosshair through it, which at any real
             * range turned into a screen of plus signs stacked on top of each other - the
             * marker was louder than the thing it was marking. Corners read as "something is
             * there" without covering it up, and they stay legible when a dozen overlap.
             */
            int arm = Math.max(2, size / 2);
            // top-left
            guiGraphics.fill(x - size, y - size, x - size + arm, y - size + 1, color);
            guiGraphics.fill(x - size, y - size, x - size + 1, y - size + arm, color);
            // top-right
            guiGraphics.fill(x + size - arm, y - size, x + size, y - size + 1, color);
            guiGraphics.fill(x + size - 1, y - size, x + size, y - size + arm, color);
            // bottom-left
            guiGraphics.fill(x - size, y + size - 1, x - size + arm, y + size, color);
            guiGraphics.fill(x - size, y + size - arm, x - size + 1, y + size, color);
            // bottom-right
            guiGraphics.fill(x + size - arm, y + size - 1, x + size, y + size, color);
            guiGraphics.fill(x + size - 1, y + size - arm, x + size, y + size, color);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Whether a chakra sense should bother reporting this.
     *
     * It used to report every living thing in range, which at forty-eight blocks means every
     * cow, sheep, chicken, fish and squid in a small village - the screen in the report was
     * mostly markers on water. A chakra sense is for finding people and things that can hurt
     * you, so passive vanilla animals are dropped and everything else stays.
     */
    private static boolean worthMarking(LivingEntity entity) {
        if (entity instanceof Player) {
            return true;
        }
        return !(entity instanceof net.minecraft.world.entity.animal.Animal)
                && !(entity instanceof net.minecraft.world.entity.animal.WaterAnimal)
                && !(entity instanceof net.minecraft.world.entity.ambient.AmbientCreature);
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // Any chakra sense drives this now, not the Byakugan alone: the Sharingan reads
            // chakra at short range, Kurama lends his own senses, and senjutsu reaches further
            // than any eye - which is how Naruto found Nagato when the village could not.
            this.range = ninjaData.getChakraSightRange();
            this.active = this.range > 0;
        });

        if (!this.active || player.tickCount % 10 != 0) {
            if (!this.active) {
                this.visibleEntities.clear();
            }
            return;
        }

        // The box is capped even when the range is not: the client is only told about
        // entities in its own loaded chunks, so a wider sweep costs work and finds nothing.
        // The range itself still drives the distance test and the marker fade.
        AABB search = player.getBoundingBox().inflate(Math.min(this.range, MAX_SEARCH_RADIUS));
        Vec3 playerPos = player.position();
        this.visibleEntities.clear();
        this.visibleEntities.addAll(player.level().getEntitiesOfClass(LivingEntity.class, search, entity ->
                        entity != player
                                && entity.isAlive()
                                && !entity.isSpectator()
                                && !entity.isInvisible()
                                && worthMarking(entity)
                                && entity.distanceToSqr(player) <= (double) this.range * this.range)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(playerPos)))
                .limit(MAX_TRACKED_ENTITIES)
                .toList());
    }
}
