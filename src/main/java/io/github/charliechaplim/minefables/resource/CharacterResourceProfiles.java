package io.github.charliechaplim.minefables.resource;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Perfis de recursos. Personagem nao classificado != personagem sem energia. */
public final class CharacterResourceProfiles {
    private static final Map<String, Set<ResourceType>> PROFILES = new HashMap<>();

    static {
        add(ResourceType.STAMINA, "william");
        add(ResourceType.MANA, "kalista", "roy", "teste");
        add(ResourceType.STACK, "lastia");
        add(ResourceType.CURSED, "nalik", "asphodel", "pellokia", "kimiramatsu");
        add(ResourceType.BLOOD, "kimiramatsu");
        add(ResourceType.ELECTRIC, "kimiramatsu");
        // Lass e explicitamente sem barra.
        noEnergy("lass");
    }

    private CharacterResourceProfiles() {}

    private static void add(ResourceType resource, String... ids) {
        for (String id : ids) {
            PROFILES.computeIfAbsent(id, unused -> EnumSet.noneOf(ResourceType.class)).add(resource);
        }
    }

    private static void noEnergy(String... ids) {
        for (String id : ids) {
            PROFILES.put(id, EnumSet.noneOf(ResourceType.class));
        }
    }

    public static Set<ResourceType> forCharacter(ResourceLocation id) {
        if (id == null || !"minefables".equals(id.getNamespace())) return Set.of();
        return Set.copyOf(PROFILES.getOrDefault(id.getPath(), Set.of()));
    }

    public static boolean hasProfile(ResourceLocation id) {
        return id != null && "minefables".equals(id.getNamespace())
                && PROFILES.containsKey(id.getPath());
    }
}
