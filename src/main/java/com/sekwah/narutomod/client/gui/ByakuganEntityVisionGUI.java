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
            int size = Math.max(5, (int) (18.0D / Math.max(1.0D, distance / 20.0D)));
            int alpha = Math.max(90, 190 - (int) Math.min(100, distance / Math.max(1, this.range) * 100));
            int color = (alpha << 24) | 0xBFEFFF;
            guiGraphics.fill(x - size, y - 1, x + size, y + 1, color);
            guiGraphics.fill(x - 1, y - size, x + 1, y + size, color);
            guiGraphics.fill(x - size, y - size, x + size, y - size + 1, color);
            guiGraphics.fill(x - size, y + size - 1, x + size, y + size, color);
            guiGraphics.fill(x - size, y - size, x - size + 1, y + size, color);
            guiGraphics.fill(x + size - 1, y - size, x + size, y + size, color);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
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
                                && entity.distanceToSqr(player) <= (double) this.range * this.range)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(playerPos)))
                .limit(MAX_TRACKED_ENTITIES)
                .toList());
    }
}
