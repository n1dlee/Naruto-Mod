package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.LightningCoat;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT)
public final class LightningArmorRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<Player, Track> TRACKS = new WeakHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Section LEGS = new Section(0.185, 0.185, 0.74, 0.74, 0);
    private static final Section ARMS = new Section(0.185, 0.185, 0.67, 0.74, 0.29f);
    private static final Section CHEST = new Section(0.34, 0.19, 0.72, 0.72, 0.32f);
    private static final Section CROWN = new Section(0.37, 0.37, 0.45, -0.02, 0.56f);

    public LightningArmorRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft game = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || game.isPaused()) {
            return;
        }
        if (game.level == null) {
            TRACKS.clear();
            return;
        }
        TRACKS.keySet().removeIf(player -> player.isRemoved() || player.level() != game.level);
        for (Player player : game.level.players()) {
            boolean active = visible(game, player)
                    && player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> LightningCoat.active(player, data)).orElse(false);
            Track track = TRACKS.get(player);
            if (active && track == null) {
                track = new Track(player.position());
                TRACKS.put(player, track);
            }
            if (track != null && !track.step(game, player, active)) {
                TRACKS.remove(player);
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float swing, float swingAmount, float partial, float age, float yaw, float pitch) {
        Track track = TRACKS.get(player);
        if (track == null || !visible(Minecraft.getInstance(), player)) {
            return;
        }
        var model = getParentModel();
        var coat = new Pass(pose, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), track, partial, low(player), age);
        coat.limb(model.rightLeg, LEGS);
        coat.limb(model.leftLeg, LEGS);
        coat.limb(model.body, CHEST);
        coat.limb(model.rightArm, ARMS);
        coat.limb(model.leftArm, ARMS);
        if (!coat.low()) {
            coat.limb(model.head, CROWN);
        }
    }

    public static void firstPerson(AbstractClientPlayer player, ModelPart arm, PoseStack pose,
                                   MultiBufferSource buffers, float partial) {
        Track track = TRACKS.get(player);
        if (track != null) {
            var coat = new Pass(pose, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW),
                    track, partial, low(player), player.tickCount + partial);
            coat.limb(arm, ARMS);
        }
    }

    static void trails(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        Mesh mesh = null;
        var game = Minecraft.getInstance();
        for (Map.Entry<Player, Track> entry : TRACKS.entrySet()) {
            Player player = entry.getKey();
            Track t = entry.getValue();
            if (player.level() != level || !visible(game, player) || t.intensity <= 0
                    || t.tip.distanceToSqr(camera) > 40 * 40) {
                continue;
            }
            Vec3 tail = t.tail;
            Vec3 tip = t.tip;
            if (mesh == null) {
                mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
            }
            Vec3 lateral = tip.subtract(tail).cross(UP).normalize().scale(0.27);
            Vec3 height = UP.scale(player.getBbHeight() * 0.59);
            float alpha = t.intensity * (0.75f + partial * 0.25f);
            mesh.strip(tail.add(height).add(lateral).subtract(camera), tip.add(height).add(lateral).subtract(camera),
                    UP.scale(0.034), alpha * 0.68f);
            if (!low(player)) {
                mesh.strip(tail.add(height).subtract(lateral).subtract(camera), tip.add(height).subtract(lateral).subtract(camera),
                        UP.scale(0.031), alpha * 0.61f);
            }
        }
        if (mesh != null) {
            buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
        }
    }

    private static boolean visible(Minecraft game, Player player) {
        return game.player != null && !player.isSpectator() && !player.isInvisibleTo(game.player);
    }

    private static boolean low(Player player) {
        var game = Minecraft.getInstance();
        return game.options.graphicsMode().get() == GraphicsStatus.FAST
                || (game.getCameraEntity() != null && game.getCameraEntity().distanceToSqr(player) > 25 * 25);
    }

    private record Section(double width, double depth, double height, double bottom, float starts) {
        private Vec3 ring(double angle, double offset, double y) {
            return new Vec3(Math.cos(angle) * (this.width + offset), y, Math.sin(angle) * (this.depth + offset));
        }
    }

    private record Pass(PoseStack pose, VertexConsumer sink, Track track, float partial, boolean low, float age) {
        private void limb(ModelPart part, Section section) {
            float layer = LightningCoat.layer(track.age, section.starts()) * (track.fade / LightningCoat.FADE);
            if (layer <= 0.005f || !part.visible) {
                return;
            }
            pose.pushPose();
            try {
                part.translateAndRotate(pose);
                surface(new Mesh(pose.last().pose(), sink), section, layer);
            } finally {
                pose.popPose();
            }
        }

        private void surface(Mesh mesh, Section section, float layer) {
            int count = low ? 2 : 4;
            float clock = age + partial;
            float stress = 0.64f + 0.36f * Mth.sin(clock * 0.37f + (float) section.width() * 7);
            for (int n = 0; n < count; n++) {
                double turn = n * 2.0 * Math.PI / count + Math.floor(clock * 0.36) * 0.39;
                double rise = section.height() * layer;
                Vec3 root = section.ring(turn, 0, section.bottom());
                Vec3 neck = section.ring(turn + 0.72, 0.022, section.bottom() - rise * 0.43);
                Vec3 tip = section.ring(turn + 0.44, 0.014, section.bottom() - rise);
                Vec3 edge = new Vec3(-Math.sin(turn), 0, Math.cos(turn)).scale(0.022);
                mesh.strip(root, neck, edge, layer * stress);
                mesh.strip(neck, tip, edge.scale(0.74), layer * stress);
            }
        }

    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void strip(Vec3 from, Vec3 to, Vec3 side, float alpha) {
            Vec3 blur = side.scale(2.8);
            quad(from, to, blur, 0.17f, 0.56f, alpha * 0.31f);
            quad(from, to, side, 0.69f, 0.93f, alpha);
        }

        private void quad(Vec3 from, Vec3 to, Vec3 side, float red, float green, float alpha) {
            vertex(from.add(side), red, green, alpha * 0.37f);
            vertex(from.subtract(side), red, green, alpha * 0.37f);
            vertex(to.subtract(side), red, green, alpha);
            vertex(to.add(side), red, green, alpha);
        }

        private void vertex(Vec3 p, float red, float green, float alpha) {
            buffer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(red, green, 1, alpha).endVertex();
        }
    }

    private static final class Track {
        private Vec3 tip;
        private Vec3 tail;
        private Vec3 lastPosition;
        private float age;
        private float fade;
        private float intensity;
        private Hum hum;

        private Track(Vec3 position) {
            this.tip = position;
            this.tail = position;
            this.lastPosition = position;
        }

        private boolean step(Minecraft game, Player player, boolean active) {
            this.fade = active ? LightningCoat.FADE : Math.max(0, this.fade - 1);
            this.age = active ? Math.min(LightningCoat.FORM, this.age + 1) : this.age;
            Vec3 next = player.position();
            Vec3 dt = next.subtract(this.lastPosition);
            if (active && LightningCoat.leavesTrail(dt, player.isSprinting())) {
                this.tail = this.lastPosition;
                this.tip = next;
                this.intensity = Math.min(1f, this.intensity + 0.37f);
            } else if (dt.lengthSqr() >= 1.85 * 1.85) {
                this.intensity = 0;
                this.tail = next;
                this.tip = next;
            } else {
                this.intensity = Math.max(0, this.intensity - 0.29f);
            }
            this.lastPosition = next;
            if (active && (this.hum == null || this.hum.isStopped()) && game.getCameraEntity() != null
                    && game.getCameraEntity().distanceToSqr(player) <= 18 * 18) {
                this.hum = new Hum(player, this);
                game.getSoundManager().play(this.hum);
            }
            return this.fade > 0;
        }
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private final WeakReference<Player> player;
        private final Track track;

        private Hum(Player player, Track track) {
            super(NarutoSounds.LIGHTNING_COAT_HUM.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = new WeakReference<>(player);
            this.track = track;
            this.looping = true;
            this.volume = 0.01f;
        }

        @Override
        public void tick() {
            Player owner = this.player.get();
            Minecraft game = Minecraft.getInstance();
            if (owner == null || owner.isRemoved() || owner.level() != game.level || this.track.fade <= 0
                    || !visible(game, owner) || game.getCameraEntity() == null
                    || game.getCameraEntity().distanceToSqr(owner) > 21 * 21) {
                this.stop();
                return;
            }
            this.x = owner.getX();
            this.y = owner.getY() + owner.getBbHeight() * 0.6;
            this.z = owner.getZ();
            this.volume = 0.24f * LightningCoat.layer(this.track.age, 0) * (this.track.fade / LightningCoat.FADE);
            this.pitch = 0.92f + this.track.intensity * 0.19f;
        }
    }
}
