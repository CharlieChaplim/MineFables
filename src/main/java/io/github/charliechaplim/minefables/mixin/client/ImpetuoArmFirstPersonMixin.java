package io.github.charliechaplim.minefables.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.charliechaplim.minefables.client.ClientWilliamState;
import io.github.charliechaplim.minefables.client.render.ImpetuoFirstPerson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerRenderer ja aplicou o transform real do braco de primeira pessoa.
 * Renderizar aqui garante que o aro siga a mao, inclusive na animacao de golpe.
 */
@Mixin(PlayerRenderer.class)
public abstract class ImpetuoArmFirstPersonMixin {
    @Inject(method = "renderHand", at = @At("TAIL"))
    private void minefables$renderWristRing(PoseStack pose, MultiBufferSource buffers, int packedLight,
                                            AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                            CallbackInfo ci) {
        if (player != Minecraft.getInstance().player || !ClientWilliamState.prepared()
                || player.isInvisible()
                || !player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty()) return;

        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        ModelPart mainArm = player.getMainArm() == HumanoidArm.RIGHT
                ? renderer.getModel().rightArm
                : renderer.getModel().leftArm;
        if (arm != mainArm) return;
        ImpetuoFirstPerson.renderOnWrist(pose, buffers, arm, player);
    }
}
