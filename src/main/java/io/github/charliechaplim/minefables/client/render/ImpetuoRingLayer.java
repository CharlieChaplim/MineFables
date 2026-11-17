package io.github.charliechaplim.minefables.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.charliechaplim.minefables.client.ClientImpetuoState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Aro acompanha exatamente a rotação do braço, inclusive ataques e caminhada. */
public final class ImpetuoRingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public ImpetuoRingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void register(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new ImpetuoRingLayer(renderer));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientImpetuoState.isArmed(player) || player.isInvisible()) return;
        ModelPart arm = player.getMainArm() == HumanoidArm.RIGHT ? getParentModel().rightArm : getParentModel().leftArm;
        if (!arm.visible) return;
        pose.pushPose();
        arm.translateAndRotate(pose);
        // Origem do braço vanilla no ombro: 8.5 px abaixo = região do punho.
        pose.translate(0, 8.5 / 16.0, 0);
        ImpetuoRingRenderer.render(pose, buffers, ageInTicks);
        pose.popPose();
    }
}
