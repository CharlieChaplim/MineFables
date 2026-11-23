package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.ability.AbilityDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Desenha os PNGs de 32x32 que o autor entregou (sem gerar imagens extras). */
public final class AbilityIcon {
    private AbilityIcon(){}
    public static ResourceLocation texture(AbilityDefinition ability){
        return ResourceLocation.fromNamespaceAndPath(MineFables.MODID,
                "textures/gui/abilities/"+ability.id().getPath()+".png");
    }
    public static void draw(GuiGraphics gui, AbilityDefinition ability,int x,int y,int size){
        gui.pose().pushPose();
        gui.pose().translate(x,y,0);
        gui.pose().scale(size/32f,size/32f,1);
        gui.blit(texture(ability),0,0,0,0,32,32,32,32);
        gui.pose().popPose();
    }
}
