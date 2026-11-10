
package io.github.charliechaplim.minefables.character;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

public final class CharacterRegistry {

    private static final Map<ResourceLocation, CharacterDefinition> CHARACTERS =
            new LinkedHashMap<>();

    private CharacterRegistry() {
    }

    public static CharacterDefinition register(CharacterDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        CharacterDefinition previous = CHARACTERS.putIfAbsent(
                definition.id(),
                definition
        );

        if (previous != null) {
            throw new IllegalStateException(
                    "Character already registered: " + definition.id()
            );
        }

        return definition;
    }

    public static Optional<CharacterDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(CHARACTERS.get(id));
    }

    public static Collection<CharacterDefinition> getAll() {
        return List.copyOf(CHARACTERS.values());
    }
}
