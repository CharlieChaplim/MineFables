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
        add(ResourceType.STAMINA,
                "elias", "kuntiri", "draconir", "taty", "william", "lucas", "scintilla",
                "sakuya", "melly", "shinichi", "shion", "nihil", "chain", "lass", "brak",
                "ruth", "betty", "ibi", "marina", "dante");

        add(ResourceType.MANA,
                "roy", "mordekaiser", "lori", "unmei", "hayato", "kaito",
                "luck", "pato", "kalista", "ainz", "rei_helstea", "teste");

        add(ResourceType.FAITH,
                "akayuki", "junin", "padre_joao", "padre_marcelo_rossi", "sumeragi",
                "tamaiel", "ryuuko");

        add(ResourceType.BLOOD,
                "amnesia", "kimiramatsu", "unmei", "shinichi", "madara", "tatsu", "ashley");

        // Roy: origem Jujutsu com Restricao Celestial, sem barra Amaldicoada.
        add(ResourceType.CURSED, "kimiramatsu", "nalik", "asphodel", "outis");
        add(ResourceType.ELECTRIC, "chuai_xeng", "arisu");
        add(ResourceType.STACK, "wayne", "lastia");

        // Realmente nao usam barra nem custo de recurso.
        noEnergy("pwie", "bort", "tobi", "cross", "guinevere");

        // Ainda sem classificacao confirmada: daniel, paracelcius, sophia, luke.
        // Nao criar perfil vazio para eles: isso os classificaria incorretamente.
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
