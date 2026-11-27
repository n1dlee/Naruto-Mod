package com.sekwah.narutomod.client.gui;

import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.SharinganReading;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Comparator;
import java.util.Set;

public class SharinganOverlayGUI implements PlayerGUI {
    private static final String PATH = "textures/effects/sharingan/";
    private static final String[] CRESTS = {"", "sharingan_1", "sharingan_2", "sharingan_3", "mangekyou"};
    private static final Set<String> EMS_FORMS = Set.of("itachi", "sasuke", "madara", "shisui", "obito");
    private final Minecraft minecraft;
    private Player owner;
    private LivingEntity tracked;
    private int age;
    private int level;
    private String ems = "";
    private Component copy;

    public SharinganOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics gui, Matrix4f worldMatrix, Vec3 camera) {
        if (this.level <= 0 || this.owner != this.minecraft.player) {
            return;
        }
        int w = gui.guiWidth();
        int h = gui.guiHeight();
        boolean firstPerson = this.minecraft.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            this.drawEdges(gui, w, h);
        }
        this.icon(gui, w);
        this.drawCopy(gui, w);
        if (firstPerson && worldMatrix != null && camera != null) {
            this.drawTrajectory(gui, new View(worldMatrix, camera, w, h));
        }
    }

    private void drawEdges(GuiGraphics gui, int w, int h) {
        int rim = Math.min(30, Math.max(12, h / 6));
        int strength = 21 + Math.round(20 * Math.min(this.age / 5f, 1));
        int red = strength << 24 | 0x7A1E27;
        gui.fillGradient(0, 0, w, rim, red, 0x007A1E27);
        gui.fillGradient(0, h - rim, w, h, 0x007A1E27, red);
    }

    private void icon(GuiGraphics gui, int width) {
        String name = this.ems.isEmpty() ? CRESTS[Math.min(this.level, 4)] : "ems_" + this.ems;
        var texture = new ResourceLocation(NarutoMod.MOD_ID, PATH + name + ".png");
        int size = this.level >= 4 ? 43 : 38;
        int x = width - size - 8;
        gui.pose().pushPose();
        try {
            gui.pose().translate(x + size * 0.5, 8 + size * 0.5, 0);
            gui.pose().mulPose(Axis.ZP.rotationDegrees(SharinganReading.turn(this.age + this.minecraft.getFrameTime())));
            gui.blit(texture, -size / 2, -size / 2, 0, 0, size, size, size, size);
        } finally {
            gui.pose().popPose();
        }
    }

    private void drawCopy(GuiGraphics gui, int width) {
        if (this.copy == null) {
            return;
        }
        String title = this.minecraft.font.plainSubstrByWidth(this.copy.getString(), Math.min(177, width / 2));
        int x = width - 9 - this.minecraft.font.width(title);
        gui.fill(x - 6, 57, width - 6, 71, 0xB91E1219);
        gui.fill(x - 6, 57, x - 5, 71, 0xCFDA566A);
        gui.drawString(this.minecraft.font, title, x, 60, 0xFFE9BCC7, false);
    }

    private void drawTrajectory(GuiGraphics gui, View view) {
        LivingEntity target = this.tracked;
        if (target == null || !target.isAlive() || target.isInvisibleTo(this.owner)
                || !this.owner.hasLineOfSight(target)) {
            return;
        }
        Vec3 center = target.getPosition(this.minecraft.getFrameTime()).add(0, target.getBbHeight() * 0.64, 0);
        Vec3 observed = target.position().subtract(target.xo, target.yo, target.zo);
        int count = this.minecraft.options.graphicsMode().get() == GraphicsStatus.FAST ? 1 : 2;
        for (int i = 1; i <= count; i++) {
            Vec3 shift = SharinganReading.lead(observed, i * 2.5);
            if (shift.lengthSqr() < 0.04) {
                return;
            }
            Vec3 lead = center.add(shift);
            if (this.owner.level().clip(new ClipContext(center, lead, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.BLOCK) {
                break;
            }
            view.drawMark(gui, lead, i);
        }
    }

    @Override
    public void tick(Player player) {
        if (this.owner != player) {
            this.owner = player;
            this.level = 0;
            this.tracked = null;
        }
        var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null || !data.isNinjaModeEnabled() || !data.isSharinganActive() || !player.isAlive()) {
            this.level = 0;
            this.age = 0;
            this.tracked = null;
            this.copy = null;
            return;
        }
        int next = data.getSharinganLevel();
        this.age = next == this.level ? Math.min(40, this.age + 1) : 0;
        this.level = next;
        this.ems = data.isEternalMangekyoAwakened() && EMS_FORMS.contains(data.getMangekyoForm()) ? data.getMangekyoForm() : "";
        var key = ResourceLocation.tryParse(NarutoMod.MOD_ID + ":" + data.getCopiedJutsu());
        var ability = key == null ? null : NarutoRegistries.ABILITIES.getValue(key);
        this.copy = ability != null && data.isJutsuLearned(data.getCopiedJutsu())
                ? Component.translatable("sharingan.copy.ready", Component.translatable(ability.getTranslationKey(data))) : null;
        if (player.tickCount % 4 == 0 || this.age == 0) {
            this.tracked = focus(player);
        }
    }

    private static LivingEntity focus(Player player) {
        var eye = player.getEyePosition();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SharinganReading.RANGE),
                target -> target != player && target.isAlive() && !target.isSpectator() && !target.isInvisibleTo(player)
                        && !player.isAlliedTo(target) && !Faction.sameSide(player, target)
                        && (target instanceof Player || target instanceof net.minecraft.world.entity.monster.Enemy)
                        && SharinganReading.watching(player.getLookAngle(), target.getEyePosition().subtract(eye))
                        && player.hasLineOfSight(target))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private record View(Matrix4f matrix, Vec3 camera, int width, int height) {
        private void drawMark(GuiGraphics gui, Vec3 point, int step) {
            Vec3 relative = point.subtract(camera);
            var screen = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1).mul(matrix);
            if (screen.w <= 0 || Math.abs(screen.x) >= screen.w || Math.abs(screen.y) >= screen.w) {
                return;
            }
            int x = Mth.floor((1 + screen.x / screen.w) * width * 0.5f);
            int y = Mth.floor((1 - screen.y / screen.w) * height * 0.5f);
            int color = step == 1 ? 0x8AE06B80 : 0x48E06B80;
            gui.fill(x - 4, y - 3, x - 2, y + 3, color);
            gui.fill(x + 2, y - 3, x + 4, y + 3, color);
            gui.fill(x - 2, y - 4, x + 2, y - 3, color);
        }
    }
}
