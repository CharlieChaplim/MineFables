package io.github.charliechaplim.minefables.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Um único aro dourado, modelado por quads. Não requer Photon nem GeckoLib. */
public final class ImpetuoRingRenderer {
    private static final ResourceLocation PIXEL = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "textures/effect/gold_pixel.png");
    private static final RenderType TYPE = RenderType.entityTranslucent(PIXEL);
    private static final int SEGMENTS = 32;

    private ImpetuoRingRenderer() {}

    /** Coordenadas locais: eixo Y acompanha o braço. */
    public static void render(PoseStack pose, MultiBufferSource buffers, float age) {
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        float pulse = 1.0f + 0.035f * (float) Math.sin(age * 0.22);
        // Halo discreto e aro central brilhante, com iluminação total.
        band(pose, consumer, .247f * pulse, .08f, 255, 178, 31, 75);
        band(pose, consumer, .239f * pulse, .032f, 255, 219, 66, 235);
        // Pequenas faíscas orbitam ao longo do aro.
        for (int i = 0; i < 3; i++) {
            float center = age * .085f + i * (float) (Math.PI * 2.0 / 3.0);
            arc(pose, consumer, .254f * pulse, .050f, center - .085f, center + .085f);
        }
    }

    private static void band(PoseStack pose, VertexConsumer out, float radius, float height,
                             int r, int g, int b, int alpha) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a = (float) (i * 2.0 * Math.PI / SEGMENTS);
            float next = (float) ((i + 1) * 2.0 * Math.PI / SEGMENTS);
            quad(pose, out, radius, height, a, next, r, g, b, alpha);
        }
    }

    private static void arc(PoseStack pose, VertexConsumer out, float radius, float height,
                            float from, float to) {
        quad(pose, out, radius, height, from, to, 255, 251, 200, 255);
    }

    private static void quad(PoseStack pose, VertexConsumer out, float radius, float height,
                             float start, float end, int r, int g, int b, int a) {
        float x0 = (float) Math.cos(start), z0 = (float) Math.sin(start);
        float x1 = (float) Math.cos(end), z1 = (float) Math.sin(end);
        float top = -height * .5f, bottom = height * .5f;
        put(pose, out, radius * x0, top, radius * z0, 0f, 0f, x0, z0, r, g, b, a);
        put(pose, out, radius * x0, bottom, radius * z0, 0f, 1f, x0, z0, r, g, b, a);
        put(pose, out, radius * x1, bottom, radius * z1, 1f, 1f, x1, z1, r, g, b, a);
        put(pose, out, radius * x1, top, radius * z1, 1f, 0f, x1, z1, r, g, b, a);
    }

    private static void put(PoseStack pose, VertexConsumer out, float x, float y, float z,
                            float u, float v, float nx, float nz, int r, int g, int b, int a) {
        PoseStack.Pose transform = pose.last();
        out.addVertex(transform.pose(), x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transform, nx, 0f, nz);
    }
}
