package io.github.charliechaplim.minefables.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.charliechaplim.minefables.network.UseAbilityPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** R = abrir roda (segurar), T = usar, [ e ] = selecionar sem abrir roda. */
public final class AbilityKeys {
    private static final String CATEGORY="key.categories.minefables";
    private static final KeyMapping PREVIOUS=new KeyMapping("key.minefables.ability_previous",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_LEFT_BRACKET,CATEGORY);
    private static final KeyMapping NEXT=new KeyMapping("key.minefables.ability_next",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_BRACKET,CATEGORY);
    private static final KeyMapping USE=new KeyMapping("key.minefables.ability_use",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_T,CATEGORY);
    private static final KeyMapping WHEEL=new KeyMapping("key.minefables.ability_wheel",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,CATEGORY);
    private AbilityKeys(){}
    public static void register(RegisterKeyMappingsEvent event){event.register(PREVIOUS);event.register(NEXT);event.register(USE);event.register(WHEEL);}
    public static boolean wheelKey(int key,int scan){return WHEEL.matches(key,scan);}
    public static void useSelected(){
        if(ClientAbilitySelection.count()>0)
            PacketDistributor.sendToServer(new UseAbilityPayload(ClientAbilitySelection.selectedIndex(),ClientAbilitySelection.ultimateMode()));
    }
    public static void onClientTick(ClientTickEvent.Post event){
        var minecraft=Minecraft.getInstance();boolean inGame=minecraft.player!=null && minecraft.screen==null;
        while(PREVIOUS.consumeClick())if(inGame)ClientAbilitySelection.cycle(-1);
        while(NEXT.consumeClick())if(inGame)ClientAbilitySelection.cycle(1);
        while(USE.consumeClick())if(inGame)useSelected();
        while(WHEEL.consumeClick())if(inGame && ClientAbilitySelection.count()>0)
            minecraft.setScreen(new AbilityWheelScreen());
    }
}
