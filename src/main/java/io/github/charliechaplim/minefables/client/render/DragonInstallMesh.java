package io.github.charliechaplim.minefables.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Mesh extraída do .bbmodel enviado pelo autor, com UV de cada face preservado.
 * Renderização geométrica estática: GeckoLib está disponível para adicionar animações
 * depois, sem que o modelo dependa de um item real de armadura.
 */
public final class DragonInstallMesh {
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "mesh/william/dragon_install_mesh.json");
    private static ResourceManager loadedFrom;
    private static Map<String, float[]> sections = Map.of();

    private DragonInstallMesh() {}

    private static void ensureLoaded() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (loadedFrom == manager) return;
        loadedFrom = manager;
        Map<String, float[]> parsed = new HashMap<>();
        try (var reader = new InputStreamReader(manager.open(MESH), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : root.entrySet()) {
                JsonArray array = entry.getValue().getAsJsonArray();
                float[] values = new float[array.size()];
                for (int i = 0; i < values.length; i++) values[i] = array.get(i).getAsFloat();
                if (values.length % 8 != 0) throw new IllegalArgumentException("Malformed mesh section " + entry.getKey());
                parsed.put(entry.getKey(), values);
            }
            sections = Map.copyOf(parsed);
        } catch (Exception error) {
            sections = Map.of();
            MineFables.LOGGER.error("Não foi possível carregar o modelo visual Dragon Install", error);
        }
    }

    public static void render(String part, PoseStack pose, VertexConsumer buffer, int light) {
        ensureLoaded();
        float[] section = sections.get(part);
        if (section == null) return;
        var matrix = pose.last();
        for (int i = 0; i < section.length; i += 8) {
            buffer.addVertex(matrix.pose(), section[i], section[i + 1], section[i + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(section[i + 3], section[i + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(matrix, section[i + 5], section[i + 6], section[i + 7]);
        }
    }
}
