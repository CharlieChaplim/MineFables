package io.github.charliechaplim.minefables.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.charliechaplim.minefables.client.ClientWilliamState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Anel de primeira pessoa ancorado na geometria real do braço, e nao na camera.
 * A chamada ocorre depois de o Minecraft posicionar o ModelPart da mao vazia.
 */
public final class ImpetuoFirstPerson {
    // Offset local relativo ao ModelPart do braco, NAO a camera.
    // Para destros, X negativo move o anel para o lado esquerdo da tela;
    // para canhotos, o deslocamento e espelhado.
    private static final float WRIST_X_OFFSET = -0.035F;
    // Y positivo desce um pouco ao longo do braco (eixo do modelo do Minecraft).
    private static final float WRIST_Y_OFFSET = 0.030F;

    private ImpetuoFirstPerson() {}

    public static void renderOnWrist(PoseStack pose, MultiBufferSource buffers,
                                     ModelPart arm, AbstractClientPlayer player) {
        if (!ClientWilliamState.prepared() || !arm.visible) return;
        pose.pushPose();
        try {
            // Mesma transformacao usada por ModelPart.render() do braco vanilla.
            arm.translateAndRotate(pose);
            // O braco vanilla tem 12 pixels do ombro ao punho; posicao a 8,5 px do pivot.
            int side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
            pose.translate(side * WRIST_X_OFFSET, 8.5 / 16.0 + WRIST_Y_OFFSET, 0.0);
            // Raio final ~2,7 pixels: pouco acima da largura de 4 px do braco.
            pose.scale(0.68F, 0.68F, 0.68F);
            ImpetuoRingRenderer.render(pose, buffers, player.tickCount);
        } finally {
            pose.popPose();
        }
    }
}
