package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.ability.AbilityDefinition;
import io.github.charliechaplim.minefables.ability.AbilityRegistry;
import io.github.charliechaplim.minefables.resource.CharacterResourceProfiles;
import io.github.charliechaplim.minefables.resource.ResourceType;
import io.github.charliechaplim.minefables.william.WilliamKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.Set;
import java.util.List;

/** HUD com as sete barras fornecidas em barras.png (atlas RGBA 256 x 256). */
public final class ResourceHud {
    private static final ResourceLocation BARS = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "textures/gui/barras.png"
    );

    private static final int TEXTURE_SIZE = 256;
    private static final int BAR_WIDTH = 98;
    private static final int BAR_HEIGHT = 16;
    private static final int FILL_WIDTH = 92;
    private static final int FILL_HEIGHT = 13;
    private static final int ROW_SPACING = 26;

    private ResourceHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "resources"),
                (graphics, deltaTracker) -> render(graphics)
        );
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        // A HUD nao deve cobrir chat, comandos, inventario ou outras telas.
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui
                || minecraft.getDebugOverlay().showDebugScreen()) return;

        ResourceLocation character = ResourceLocation.tryParse(ClientResourceState.characterId());
        if (character == null) return;

        Set<ResourceType> available = CharacterResourceProfiles.forCharacter(character);
        if (available.isEmpty()) return;

        int x = 8;
        // Empilha as barras para cima, longe da parte inferior da HUD vanilla.
        int y = graphics.guiHeight() - 119 - ROW_SPACING * (available.size() - 1);
        for (ResourceType type : ResourceType.values()) {
            if (!available.contains(type)) continue;

            long maximum = type.baseCapacity();
            long current = Math.max(0L, Math.min(maximum,
                    ClientResourceState.values().getOrDefault(type.id(), type.initialValue())));

            drawBar(graphics, type, x, y, current, maximum);
            // Somente os numeros acima da barra: sem texto na lateral.
            String numbers = current + "/" + maximum;
            graphics.drawString(minecraft.font, numbers,
                    x + 4, y - 10, 0xFFFFFFFF, true);
            y += ROW_SPACING;
        }
        // Posiciona o nome da habilidade DEPOIS da ultima barra (inclui Mana).
        renderSelectedAbility(graphics, minecraft, character, x, y + 2);
        if (WilliamKit.ID.equals(character)) renderWilliamStatus(graphics, minecraft, x);
    }

    private static void renderSelectedAbility(GuiGraphics graphics, Minecraft minecraft,
                                               ResourceLocation characterId, int x, int y) {
        int level=WilliamKit.ID.equals(characterId)?ClientWilliamState.level():Integer.MAX_VALUE;
        boolean ult=WilliamKit.ID.equals(characterId)&&ClientWilliamState.ultimate();
        List<AbilityDefinition> abilities=AbilityRegistry.forCharacter(characterId,level,ult);
        if(abilities.isEmpty())return;
        int index=Math.min(ClientAbilitySelection.selectedIndex(),abilities.size()-1);
        int prev=Math.floorMod(index-1,abilities.size()),next=(index+1)%abilities.size();
        // Textura original da HUD de tres icones: anterior / atual / proximo.
        ResourceLocation panel=ResourceLocation.fromNamespaceAndPath(MineFables.MODID,"textures/gui/gui_habilidades.png");
        graphics.blit(panel,x,y,0,0,96,64,96,64);
        AbilityIcon.draw(graphics,abilities.get(prev),x+10,y+23,18);
        AbilityIcon.draw(graphics,abilities.get(index),x+36,y+20,24);
        AbilityIcon.draw(graphics,abilities.get(next),x+68,y+23,18);
        for(int i=0;i<3;i++){
            int pos=i==0?prev:i==1?index:next;
            int cd=ClientWilliamState.cooldownTicks(pos,ult);
            if(cd<=0)continue;
            int ix=x+(i==0?10:i==1?36:68),iy=y+(i==1?20:23),size=i==1?24:18;
            graphics.fill(ix,iy,ix+size,iy+size,0xA0999999);
            graphics.drawCenteredString(minecraft.font,Integer.toString((cd+19)/20),ix+size/2,iy+6,0xFFFFFFFF);
        }
        String label=Component.translatable(abilities.get(index).nameTranslationKey()).getString();
        graphics.drawString(minecraft.font,label,x+3,y+50,0xFFFFFFFF,true);
    }

    private static void renderWilliamStatus(GuiGraphics graphics, Minecraft minecraft, int barX) {
        int y = graphics.guiHeight() - 64;
        if (ClientWilliamState.tier() > 0) {
            graphics.drawString(minecraft.font, "RESSONÂNCIA " + switch (ClientWilliamState.tier()) {
                case 2 -> "II";
                case 3 -> "III";
                default -> "I";
            }, barX + 105, y, 0xFFFF6666, true);
        }
        if (ClientWilliamState.prepared()) {
            graphics.drawString(minecraft.font, "IMPÉTUO ARMADO", barX + 105, y + 12, 0xFFFFD86C, true);
        }
        if (!ClientWilliamState.ultimate()) return;

        // Faixa permanente da Ultimate: legivel mesmo ao alternar habilidades.
        int cx = graphics.guiWidth() / 2;
        int top = 16;
        String title = "✦ REESCRITA DRACÔNICA ✦";
        String timer = "TEMPO RESTANTE: " + ClientWilliamState.seconds() + "s";
        int w = Math.max(minecraft.font.width(title), minecraft.font.width(timer)) + 40;
        graphics.fill(cx - w / 2, top - 5, cx + w / 2, top + 34, 0xC0200905);
        graphics.fill(cx - w / 2, top - 5, cx + w / 2, top - 3, 0xFFFF6330);
        graphics.drawCenteredString(minecraft.font, title, cx, top, 0xFFFFA04A);
        graphics.drawCenteredString(minecraft.font, timer, cx, top + 13, 0xFFFFFFFF);
        // 60 segundos base, prorrogados por Sobrecarga.
        int width = Math.max(0, Math.min(w - 12, Math.round((w - 12) *
                (Math.min(ClientWilliamState.seconds(), 60) / 60.0f))));
        graphics.fill(cx - (w - 12) / 2, top + 27,
                cx - (w - 12) / 2 + width, top + 30, 0xFFFF7439);
        ResourceLocation heat=ResourceLocation.fromNamespaceAndPath(MineFables.MODID,"textures/gui/temperatura.png");
        // Barra de temperatura ao lado direito do contador da ultimate.
        int heatX = cx + w/2 + 8;
        graphics.blit(heat,heatX,top-4,ClientWilliamState.temperature()>=4?32f:0f,0f,32,64,64,64);
        for (int i=0; i<4; i++) graphics.fill(heatX + i*8,top+62,heatX+i*8+7,top+68,
                i<ClientWilliamState.temperature()?0xFFFF6935:0xAA33445A);
        graphics.drawCenteredString(minecraft.font,ClientWilliamState.temperature()+"/4",
                heatX+16,top+72,0xFFFFFFFF);
        if (ClientWilliamState.temperature()>=4)
            graphics.drawCenteredString(minecraft.font,"SUPERAQUECIDO — SOBRECARGA",cx,top+39,0xFFFF6740);
    }

    private static void drawBar(GuiGraphics gui, ResourceType type,
                                int x, int y, long current, long maximum) {
        int spriteX = switch (type) {
            case MANA, STAMINA, FAITH, BLOOD -> 0;
            case CURSED, STACK, ELECTRIC -> 112;
        };
        int emptyY = switch (type) {
            case MANA, CURSED -> 0;
            case STAMINA, STACK -> 48;
            case FAITH, ELECTRIC -> 96;
            case BLOOD -> 145;
        };

        // Mantem a moldura e o cristal mesmo com o recurso zerado.
        gui.blit(BARS, x, y, (float) spriteX, (float) emptyY,
                BAR_WIDTH, BAR_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        if (maximum <= 0L || current <= 0L) return;

        // Recorta horizontalmente o sprite cheio; nunca estica pixels.
        double fraction = Math.min(1D, (double) current / (double) maximum);
        int visiblePixels = (int) Math.round(FILL_WIDTH * fraction);
        if (visiblePixels <= 0) return;

        // O sprite cheio tem 3 pixels de margem esquerda e comeca 19 px
        // abaixo do sprite vazio. Os dois cristais ficam alinhados no topo.
        gui.blit(BARS, x + 3, y, (float) (spriteX + 3), (float) (emptyY + 19),
                visiblePixels, FILL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
