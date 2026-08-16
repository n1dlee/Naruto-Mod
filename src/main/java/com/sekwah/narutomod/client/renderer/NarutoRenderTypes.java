package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types for chakra: untextured, self-lit, additive geometry.
 *
 * Particles could not do what these are for. A Rasengan is a solid sphere and Chidori is a
 * bolt with length and direction; both were being approximated with clouds of dust sprites,
 * which reads as a haze near a hand rather than as an object in it. Drawing them as real
 * geometry means they have a silhouette, an edge, and a shape that survives being looked at.
 *
 * Extending {@link RenderType} is the only way to reach the protected {@code create} and the
 * state shards; the class is never instantiated.
 */
public final class NarutoRenderTypes extends RenderType {

    private NarutoRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload,
                              Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new UnsupportedOperationException("Holder class");
    }

    /**
     * Additive, depth-tested but not depth-writing, no culling.
     *
     * Additive so overlapping layers of the same effect brighten into a core instead of
     * flattening. No depth write so the two nested spheres never z-fight each other, and no
     * culling because both are seen from inside as well as outside.
     */
    public static final RenderType CHAKRA_GLOW = create(
            "narutomod_chakra_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(NO_LIGHTMAP)
                    .createCompositeState(false));
}
