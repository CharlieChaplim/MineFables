package io.github.charliechaplim.minefables.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.client.ClientDragonInstallState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Apenas visual: nenhuma mudança no inventário ou nos valores de armadura. */
public final class DragonInstallLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "textures/entity/william/dragon_install.png");

    public DragonInstallLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void register(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer playerRenderer = event.getSkin(skin);
            if (playerRenderer != null) playerRenderer.addLayer(new DragonInstallLayer(playerRenderer));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientDragonInstallState.isActive(player.getUUID())) return;
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var model = getParentModel();
        section(pose, model.head, "head", buffer, packedLight);
        section(pose, model.body, "body", buffer, packedLight);
        section(pose, model.rightArm, "rightArm", buffer, packedLight);
        section(pose, model.leftArm, "leftArm", buffer, packedLight);
        section(pose, model.rightLeg, "rightLeg", buffer, packedLight);
        section(pose, model.leftLeg, "leftLeg", buffer, packedLight);
    }

    private static void section(PoseStack pose, ModelPart part, String name,
                                com.mojang.blaze3d.vertex.VertexConsumer buffer, int light) {
        if (!part.visible) return;
        pose.pushPose();
        part.translateAndRotate(pose);
        DragonInstallMesh.render(name, pose, buffer, light);
        pose.popPose();
    }
}
