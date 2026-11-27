package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.ByakuganSight;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class ByakuganEntityVisionGUI implements PlayerGUI {
    private static final int MAX_SEARCH_RADIUS = 512;
    private final Minecraft minecraft;
    private List<Track> visible = List.of();
    private Player owner;
    private int range;
    private int focus;
    private boolean byakugan;

    public ByakuganEntityVisionGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics gui, Matrix4f matrix, Vec3 camera) {
        if (this.range <= 0 || matrix == null || camera == null || this.owner != this.minecraft.player
                || this.owner == null || this.owner.isSpectator()) {
            return;
        }
        Vec3 facing = Vec3.directionFromRotation(0, this.owner.yBodyRot);
        boolean low = this.minecraft.options.graphicsMode().get() == GraphicsStatus.FAST;
        var view = new View(matrix, camera, gui.guiWidth(), gui.guiHeight(), low);
        int left = 0;
        int right = 0;
        for (Track track : this.visible) {
            LivingEntity entity = track.entity();
            if (!entity.isAlive() || entity.isRemoved() || entity.isInvisibleTo(this.owner)) {
                continue;
            }
            Vec3 center = entity.getPosition(this.minecraft.getFrameTime()).add(0, entity.getBbHeight() * 0.55, 0);
            Vec3 sense = center.subtract(this.owner.getEyePosition());
            if (this.byakugan && !ByakuganSight.outsideBlindSpur(facing, sense)) {
                continue;
            }
            Pixel dot = view.project(center);
            float opacity = this.opacity(sense.length());
            if (dot != null) {
                view.centerMark(gui, dot, track, opacity, center);
            } else if (this.byakugan && this.focus > 0 && !low) {
                boolean onRight = facing.x * sense.z - facing.z * sense.x < 0;
                int row = onRight ? right++ : left++;
                if (row < 4) {
                    view.edgeMark(gui, row, onRight, opacity);
                }
            }
        }
    }

    private float opacity(double distance) {
        float depth = 1 - 0.68f * Mth.clamp((float) (distance / this.range), 0, 1);
        return this.byakugan ? depth * ByakuganSight.reveal(this.focus + 1) : depth;
    }

    @Override
    public void tick(Player player) {
        var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null || !player.isAlive() || player.isSpectator() || !data.isNinjaModeEnabled()) {
            this.range = 0;
            this.focus = 0;
            this.visible = List.of();
            return;
        }
        int oldRange = this.range;
        this.range = data.getChakraSightRange();
        int eyeRange = data.getByakuganRange();
        this.byakugan = eyeRange > 0 && eyeRange == this.range;
        this.focus = this.byakugan ? Math.min(ByakuganSight.REVEAL, this.focus + 1) : 0;
        if (this.range == 0) {
            this.visible = List.of();
        } else if (player.tickCount % 6 == 0 || this.owner != player || oldRange != this.range) {
            this.visible = this.collect(player);
        }
        this.owner = player;
    }

    private List<Track> collect(Player player) {
        int budget = this.minecraft.options.graphicsMode().get() == GraphicsStatus.FAST
                ? ByakuganSight.LOW_LIMIT : ByakuganSight.VISIBLE_LIMIT;
        Vec3 eye = player.getEyePosition();
        Vec3 body = Vec3.directionFromRotation(0, player.yBodyRot);
        var bounds = player.getBoundingBox().inflate(Math.min(this.range, MAX_SEARCH_RADIUS));
        return player.level().getEntitiesOfClass(LivingEntity.class, bounds, entity -> eligible(player, entity))
                .stream().filter(entity -> entity.distanceToSqr(player) <= (double) this.range * this.range)
                .filter(entity -> !this.byakugan || ByakuganSight.outsideBlindSpur(body, entity.getEyePosition().subtract(eye)))
                .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(budget)
                .map(entity -> new Track(entity, this.byakugan && player.hasLineOfSight(entity))).toList();
    }

    private static boolean eligible(Player player, LivingEntity entity) {
        return entity != player && entity.isAlive() && !entity.isInvisibleTo(player) && !entity.isSpectator()
                && !(entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature)
                && !player.isAlliedTo(entity) && !Faction.sameSide(player, entity);
    }

    private record View(Matrix4f matrix, Vec3 camera, int width, int height, boolean low) {
        @Nullable
        private Pixel project(Vec3 pos) {
            Vec3 local = pos.subtract(camera);
            var clip = new Vector4f((float) local.x, (float) local.y, (float) local.z, 1).mul(matrix);
            if (clip.w <= 0 || Math.abs(clip.x) >= clip.w || Math.abs(clip.y) >= clip.w) {
                return null;
            }
            return new Pixel(Mth.floor((1 + clip.x / clip.w) * width * 0.5f),
                    Mth.floor((1 - clip.y / clip.w) * height * 0.5f));
        }

        private void centerMark(GuiGraphics gui, Pixel dot, Track target, float opacity, Vec3 center) {
            int x = dot.x();
            int y = dot.y();
            int alpha = (int) ((target.open() ? 170 : 94) * opacity);
            int color = alpha << 24 | (target.open() ? 0xDFFBFF : 0x879FAE);
            int radius = target.open() ? 5 : 3;
            gui.fill(x - radius, y - radius, x - radius + 1, y + radius, color);
            gui.fill(x + radius - 1, y - radius, x + radius, y + radius, color);
            if (target.open() && !low) {
                double scale = target.entity().getBbHeight();
                for (int i = -1; i <= 1; i++) {
                    var part = this.project(center.add(0, i * scale * 0.19, 0));
                    if (part != null) {
                        gui.fill(part.x() - 1, part.y(), part.x() + 1, part.y() + 1, color);
                    }
                }
            } else {
                gui.fill(x - 1, y, x + 1, y + 1, color);
            }
        }

        private void edgeMark(GuiGraphics gui, int row, boolean right, float opacity) {
            int x = right ? width - 11 : 7;
            int y = height / 2 + row * 7 - 8;
            int color = (int) (104 * opacity) << 24 | 0xA9D6E0;
            gui.fill(x, y, x + 4, y + 1, color);
            gui.fill(right ? x + 3 : x, y - 2, right ? x + 4 : x + 1, y + 3, color);
        }
    }

    private record Track(LivingEntity entity, boolean open) {
    }

    private record Pixel(int x, int y) {
    }
}
