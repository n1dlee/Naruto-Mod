package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.ChakraFlow;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT)
public final class ChakraFlowRenderer {
    private static final Map<Player, Coat> COATS = new WeakHashMap<>();

    private ChakraFlowRenderer() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) {
            return;
        }
        if (minecraft.level == null) {
            COATS.clear();
            return;
        }
        COATS.keySet().removeIf(player -> player.level() != minecraft.level || player.isRemoved());
        for (Player player : minecraft.level.players()) {
            ItemStack stack = player.getMainHandItem();
            boolean active = !player.isSpectator() && ChakraFlow.edge(stack) != null
                    && player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(ChakraFlow::active).orElse(false);
            if (!active) {
                COATS.remove(player);
                continue;
            }
            Coat previous = COATS.get(player);
            Item item = stack.getItem();
            HumanoidArm arm = player.getMainArm();
            if (previous == null || previous.item() != item || previous.arm() != arm) {
                COATS.put(player, new Coat(item, arm, player.tickCount));
            }
        }
    }

    public static void draw(LivingEntity entity, ItemStack stack, HandRender render) {
        if (!(entity instanceof Player player) || stack.isEmpty() || !heldContext(render.display())) {
            return;
        }
        Coat coat = COATS.get(player);
        if (coat == null || coat.item() != stack.getItem() || coat.arm() != player.getMainArm()
                || render.left() != (coat.arm() == HumanoidArm.LEFT)) {
            return;
        }
        ChakraFlow.Edge edge = ChakraFlow.edge(stack);
        if (edge == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getFrameTime();
        float fill = ChakraFlow.fill(player.tickCount - coat.startTick() + partialTick);
        if (fill <= 0) {
            return;
        }
        float swing = player.swingingArm == InteractionHand.MAIN_HAND
                ? ChakraFlow.swing(player.getAttackAnim(partialTick)) : 0;
        var model = render.items().getModel(stack, player.level(), player, player.getId() + render.display().ordinal());
        if (model.isCustomRenderer()) {
            return;
        }
        render.poses().pushPose();
        try {
            ForgeHooksClient.handleCameraTransforms(render.poses(), model, render.display(), render.left());
            render.poses().translate(-0.5, -0.5, -0.5);
            VertexConsumer mesh = render.buffers().getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
            boolean detailed = minecraft.options.graphicsMode().get() != GraphicsStatus.FAST
                    && player.distanceToSqr(minecraft.getCameraEntity() == null ? player : minecraft.getCameraEntity()) < 32 * 32;
            drawEdge(edge, new BladePass(render.poses().last().pose(), mesh, detailed), fill, swing);
        } finally {
            render.poses().popPose();
        }
    }

    private static boolean heldContext(ItemDisplayContext context) {
        return context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static void drawEdge(ChakraFlow.Edge edge, BladePass pass, float fill, float swing) {
        Vec3 full = edge.tip().subtract(edge.base());
        Vec3 side = full.cross(edge.offset()).normalize();
        Vec3 end = edge.base().lerp(edge.tip(), fill);
        double width = edge.width() * (1.0 + 0.65 * swing);
        for (int sign : new int[]{-1, 1}) {
            Vec3 offset = edge.offset().scale(sign);
            var spine = new Spine(edge.base().add(offset), end.add(offset));
            pass.taper(spine, side.scale(width), new Light(205, 255, 238, 196));
            if (pass.detailed()) {
                pass.taper(spine, side.scale(width * (2.8 + swing)), new Light(78, 242, 180, 64));
                Vec3 tipStart = edge.base().lerp(edge.tip(), fill * 0.78f).add(offset);
                pass.taper(new Spine(tipStart, spine.end()), side.scale(width * 0.63), new Light(238, 255, 249, 224));
            }
        }
    }

    public record HandRender(ItemDisplayContext display, boolean left, ItemRenderer items,
                             PoseStack poses, MultiBufferSource buffers) {
    }

    private record Coat(Item item, HumanoidArm arm, int startTick) {
    }

    private record Spine(Vec3 start, Vec3 end) {
    }

    private record Light(int red, int green, int blue, int alpha) {
    }

    private record BladePass(Matrix4f matrix, VertexConsumer mesh, boolean detailed) {
        private void taper(Spine spine, Vec3 side, Light light) {
            Vec3 tip = side.scale(0.17);
            vertex(spine.start().add(side), light);
            vertex(spine.start().subtract(side), light);
            vertex(spine.end().subtract(tip), light);
            vertex(spine.end().add(tip), light);
        }

        private void vertex(Vec3 point, Light light) {
            mesh.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(light.red(), light.green(), light.blue(), light.alpha()).endVertex();
        }
    }
}
