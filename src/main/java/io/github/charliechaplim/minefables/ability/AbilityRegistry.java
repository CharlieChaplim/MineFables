package io.github.charliechaplim.minefables.ability;

import io.github.charliechaplim.minefables.character.CharacterRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Listas independentes por Fabula e modo; nivel filtra antes de definir indices. */
public final class AbilityRegistry {
    private static final Map<ResourceLocation, List<AbilityDefinition>> NORMAL = new HashMap<>();
    private static final Map<ResourceLocation, List<AbilityDefinition>> ULTIMATE = new HashMap<>();

    private AbilityRegistry() {}

    public static void register(ResourceLocation characterId, List<AbilityDefinition> abilities) {
        check(characterId, abilities);
        if (NORMAL.putIfAbsent(characterId, List.copyOf(abilities)) != null)
            throw new IllegalStateException("Habilidades ja registradas: " + characterId);
    }

    public static void registerUltimate(ResourceLocation characterId, List<AbilityDefinition> abilities) {
        check(characterId, abilities);
        if (ULTIMATE.putIfAbsent(characterId, List.copyOf(abilities)) != null)
            throw new IllegalStateException("Ultimate ja registrada: " + characterId);
    }

    private static void check(ResourceLocation id, List<AbilityDefinition> abilities) {
        Objects.requireNonNull(id, "characterId");
        Objects.requireNonNull(abilities, "abilities");
        if (CharacterRegistry.get(id).isEmpty())
            throw new IllegalArgumentException("Personagem desconhecido: " + id);
    }

    /** API anterior para interfaces nao associadas a um modo. */
    public static List<AbilityDefinition> forCharacter(ResourceLocation id) {
        return NORMAL.getOrDefault(id, List.of());
    }

    /** A ordem filtrada e a MESMA no cliente e no servidor. */
    public static List<AbilityDefinition> forCharacter(ResourceLocation id, int level, boolean ultimate) {
        List<AbilityDefinition> list = (ultimate ? ULTIMATE : NORMAL).getOrDefault(id, List.of());
        return list.stream().filter(ability -> level >= ability.minimumLevel()).toList();
    }
}
