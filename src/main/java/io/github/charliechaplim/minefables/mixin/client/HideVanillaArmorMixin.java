package io.github.charliechaplim.minefables.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.charliechaplim.minefables.client.ClientDragonInstallState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Oculta APENAS a aparência vanilla da armadura, só para jogadores em Dragon Install. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HideVanillaArmorMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void minefables$hideArmor(PoseStack pose, MultiBufferSource buffers, int packedLight,
            LivingEntity wearer, float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (wearer instanceof AbstractClientPlayer player
                && ClientDragonInstallState.isActive(player.getUUID())) {
            ci.cancel();
        }
    }
}
