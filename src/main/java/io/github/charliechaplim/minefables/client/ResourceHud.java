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
        int y = graphics.guiHeight() - 65 - ROW_SPACING * (available.size() - 1);
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
        int level = WilliamKit.ID.equals(characterId) ? ClientWilliamState.level() : Integer.MAX_VALUE;
        List<AbilityDefinition> abilities = AbilityRegistry.forCharacter(
                characterId, level, WilliamKit.ID.equals(characterId) && ClientWilliamState.ultimate());
        if (abilities.isEmpty()) return;

        int index = Math.min(ClientAbilitySelection.selectedIndex(), abilities.size() - 1);
        String label = (index + 1) + "/" + abilities.size() + "  " +
                Component.translatable(abilities.get(index).nameTranslationKey()).getString();
        int halfPadding = 4;
        // Texto no canto inferior esquerdo, logo ABAIXO da barra, sem disputar a hotbar.
        graphics.fill(x - 2, y - 2, x + minecraft.font.width(label) + halfPadding * 2,
                y + 11, 0xA0000000);
        graphics.drawString(minecraft.font, label, x + halfPadding, y, 0xFFFFFFFF, true);
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
        String title = "✦ DRAGON OVERRIDE ✦";
        String timer = "TEMPO RESTANTE: " + ClientWilliamState.seconds() + "s";
        int w = Math.max(minecraft.font.width(title), minecraft.font.width(timer)) + 40;
        graphics.fill(cx - w / 2, top - 5, cx + w / 2, top + 34, 0xC0200905);
        graphics.fill(cx - w / 2, top - 5, cx + w / 2, top - 3, 0xFFFF6330);
        graphics.drawCenteredString(minecraft.font, title, cx, top, 0xFFFFA04A);
        graphics.drawCenteredString(minecraft.font, timer, cx, top + 13, 0xFFFFFFFF);
        // 60 segundos de base; extensoes de Overdrive aparecem no contador.
        int width = Math.max(0, Math.min(w - 12, Math.round((w - 12) *
                (Math.min(ClientWilliamState.seconds(), 60) / 60.0f))));
        graphics.fill(cx - (w - 12) / 2, top + 27,
                cx - (w - 12) / 2 + width, top + 30, 0xFFFF7439);
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
