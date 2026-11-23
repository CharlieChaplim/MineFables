package io.github.charliechaplim.minefables.client;

import com.mojang.math.Axis;
import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.ability.AbilityDefinition;
import io.github.charliechaplim.minefables.ability.AbilityRegistry;
import io.github.charliechaplim.minefables.william.WilliamKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Roda circular dinamica. Fechar ao soltar R; o uso permanece em T. */
public final class AbilityWheelScreen extends Screen {
    private static final ResourceLocation RING=ResourceLocation.fromNamespaceAndPath(MineFables.MODID,"textures/gui/roda_habilidades.png");
    private int hovered=-1;
    public AbilityWheelScreen(){super(Component.literal("Habilidades"));}
    private List<AbilityDefinition> entries(){
        ResourceLocation ch=ClientAbilitySelection.currentCharacter();
        if(ch==null)return List.of();
        return AbilityRegistry.forCharacter(ch,WilliamKit.ID.equals(ch)?ClientWilliamState.level():Integer.MAX_VALUE,
                ClientAbilitySelection.ultimateMode());
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean keyReleased(int key,int scan,int mods){
        if(AbilityKeys.wheelKey(key,scan)){
            if(hovered>=0)ClientAbilitySelection.select(hovered);
            onClose();return true;
        }
        return super.keyReleased(key,scan,mods);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(hovered>=0){ClientAbilitySelection.select(hovered);onClose();return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float delta){
        gui.fill(0,0,width,height,0x87000000);
        List<AbilityDefinition> entries=entries();int n=entries.size();
        if(n==0){onClose();return;}
        int cx=width/2,cy=height/2;
        // A textura original e 512x512. Renderiza sem alterar a arte de origem.
        gui.pose().pushPose();gui.pose().translate(cx-120,cy-120,0);gui.pose().scale(240/512f,240/512f,1);
        gui.blit(RING,0,0,0,0,512,512,512,512);gui.pose().popPose();
        double dx=mouseX-cx,dy=mouseY-cy,dist=Math.hypot(dx,dy);
        // O setor 0 vai de -90° a -90°+360/n; o icone fica no meio dele.
        // Sem somar mais meio setor aqui, senao o mouse seleciona o proximo icone.
        hovered=dist>25&&dist<135
                ? Math.floorMod((int)Math.floor((Math.atan2(dy,dx)+Math.PI/2)/(Math.PI*2)*n),n)
                : -1;
        for(int i=0;i<n;i++){
            // O retangulo original ja aponta para CIMA (-Y).
            // Girar mais -90 graus colocava a divisoria em cima dos icones.
            float angle=(float)(2*Math.PI*i/n);
            gui.pose().pushPose();gui.pose().translate(cx,cy,0);gui.pose().mulPose(Axis.ZP.rotation(angle));
            gui.fill(-1,-112,1,-25,0xFF262332);
            gui.pose().popPose();
            float a=(float)(-Math.PI/2+2*Math.PI*(i+.5)/n);
            int x=cx+(int)(Math.cos(a)*81)-16,y=cy+(int)(Math.sin(a)*81)-16;
            if(i==hovered || (hovered<0&&i==ClientAbilitySelection.selectedIndex()))
                gui.fill(x-3,y-3,x+35,y+35,0xAAE0B449);
            AbilityIcon.draw(gui,entries.get(i),x,y,32);
            int cd=ClientWilliamState.cooldownTicks(i,ClientAbilitySelection.ultimateMode());
            if(cd>0){gui.fill(x,y,x+32,y+32,0xAA999999);
                gui.drawCenteredString(font,Long.toString((cd+19)/20),x+16,y+12,0xFFFFFFFF);}
        }
        int selected=hovered<0?ClientAbilitySelection.selectedIndex():hovered;
        gui.drawCenteredString(font,Component.translatable(entries.get(selected).nameTranslationKey()),cx,cy-6,0xFFFFFFFF);
        gui.drawCenteredString(font,"Solte R para selecionar · T para usar",cx,cy+138,0xFFFFFFFF);
        // Nao chamar super.render: Screen.render invoca renderBackground,
        // que aplica blur de menu no mundo. A roda e intencionalmente um overlay.
        // Esta tela nao possui widgets para renderizar aqui.
    }
}
