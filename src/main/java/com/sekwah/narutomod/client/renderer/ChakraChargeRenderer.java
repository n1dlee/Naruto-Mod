package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ChargeFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChakraChargeRenderer {
    private static final Map<Player, Flow> FLOWS = new WeakHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ChakraChargeRenderer() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) {
            return;
        }
        ClientLevel level = minecraft.level;
        if (world.get() != level) {
            FLOWS.clear();
            world = new WeakReference<>(level);
        }
        if (level == null) {
            return;
        }
        for (Player player : level.players()) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                boolean charging = data.isNinjaModeEnabled()
                        && NarutoAbilities.CHAKRA_CHARGE.getId().equals(data.getCurrentlyChanneledAbility());
                Flow flow = FLOWS.get(player);
                if (charging || flow != null) {
                    FLOWS.computeIfAbsent(player, key -> new Flow()).advance(
                            minecraft, player, charging ? data.getChargeIntensity() : 0);
                }
            });
        }
    }

    public static float weight(Entity entity, float partialTick) {
        Flow flow = entity instanceof Player player ? FLOWS.get(player) : null;
        return flow == null ? 0f : Mth.lerp(partialTick, flow.previous, flow.amplitude);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        if (level != world.get()) {
            return;
        }
        VertexConsumer buffer = null;
        for (Map.Entry<Player, Flow> entry : FLOWS.entrySet()) {
            Player player = entry.getKey();
            float amplitude = weight(player, partialTick);
            if (amplitude <= 0.01f || player.position().distanceToSqr(camera) > 32 * 32) {
                continue;
            }
            if (buffer == null) {
                buffer = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
            }
            Vec3 chest = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.66, 0)
                    .add(Vec3.directionFromRotation(0, player.yBodyRot).scale(0.21)).subtract(camera);
            stream(matrix, buffer, chest, player.tickCount + partialTick, amplitude);
        }
        if (buffer != null) {
            buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
        }
    }

    private static void stream(Matrix4f matrix, VertexConsumer buffer, Vec3 chest, float t, float strength) {
        float breath = 0.83f + 0.17f * Mth.sin(t * 0.23f);
        for (int i = 0; i < 10; i++) {
            double spin = i * Mth.TWO_PI / 10 - t * 0.16;
            float cascade = (i / 10f + t * 0.043f) % 1f;
            double fall = 0.26 + (1 - cascade) * 1.03;
            Vec3 tail = chest.add(Math.cos(spin) * fall, Mth.sin(i * 2.6f) * fall * 0.64, Math.sin(spin) * fall);
            Vec3 tip = chest.lerp(tail, 0.60 - cascade * 0.39);
            Vec3 edge = new Vec3(-Math.sin(spin), 0.21, Math.cos(spin)).scale(0.045 * (0.4 + strength));
            float alpha = strength * breath * Mth.sin(cascade * Mth.PI) * 0.55f;
            vertex(matrix, buffer, tail.add(edge), 0);
            vertex(matrix, buffer, tail.subtract(edge), alpha * 0.16f);
            vertex(matrix, buffer, tip.subtract(edge.scale(0.24)), alpha);
            vertex(matrix, buffer, tip.add(edge.scale(0.24)), alpha * 0.58f);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 p, float alpha) {
        buffer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(0.47f, 0.85f, 1f, alpha).endVertex();
    }

    private static final class Flow {
        private float previous;
        private float amplitude;
        private Breath sound;

        private void advance(Minecraft minecraft, Player player, int strength) {
            this.previous = this.amplitude;
            this.amplitude = Mth.approach(this.amplitude, strength / 4.0f,
                    strength == 0 ? 1f / ChargeFeedback.FADE_TICKS : 1f / ChargeFeedback.RAMP_TICKS);
            if (strength > 0 && (this.sound == null || this.sound.isStopped())
                    && minecraft.getCameraEntity() != null && minecraft.getCameraEntity().distanceToSqr(player) <= 24 * 24) {
                this.sound = new Breath(player, this);
                minecraft.getSoundManager().play(this.sound);
            }
        }
    }

    private static final class Breath extends AbstractTickableSoundInstance {
        private final WeakReference<Player> player;
        private final Flow flow;

        private Breath(Player player, Flow flow) {
            super(NarutoSounds.CHAKRA_CHARGE_BREATH.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = new WeakReference<>(player);
            this.flow = flow;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.015f;
        }

        @Override
        public void tick() {
            Player tracked = this.player.get();
            if (tracked == null || tracked.isRemoved() || tracked.level() != Minecraft.getInstance().level
                    || this.flow.amplitude <= 0.001f) {
                this.stop();
                return;
            }
            this.x = tracked.getX();
            this.y = tracked.getY() + tracked.getBbHeight() * 0.57;
            this.z = tracked.getZ();
            this.volume = 0.27f * this.flow.amplitude;
        }
    }
}
