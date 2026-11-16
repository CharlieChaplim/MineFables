package io.github.charliechaplim.minefables.ability;

import io.github.charliechaplim.minefables.character.CharacterRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Cada personagem tem uma lista ordenada de habilidades proprias. */
public final class AbilityRegistry {
    private static final Map<ResourceLocation, List<AbilityDefinition>> BY_CHARACTER = new HashMap<>();

    private AbilityRegistry() {}

    public static void register(ResourceLocation characterId, List<AbilityDefinition> abilities) {
        Objects.requireNonNull(characterId, "characterId");
        Objects.requireNonNull(abilities, "abilities");
        if (CharacterRegistry.get(characterId).isEmpty()) {
            throw new IllegalArgumentException("Personagem desconhecido: " + characterId);
        }
        if (BY_CHARACTER.putIfAbsent(characterId, List.copyOf(abilities)) != null) {
            throw new IllegalStateException("Habilidades ja registradas: " + characterId);
        }
    }

    public static List<AbilityDefinition> forCharacter(ResourceLocation characterId) {
        return BY_CHARACTER.getOrDefault(characterId, List.of());
    }
}
