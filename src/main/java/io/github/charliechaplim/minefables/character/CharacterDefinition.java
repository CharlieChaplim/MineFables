
package io.github.charliechaplim.minefables.character;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public record CharacterDefinition(
        ResourceLocation id,
        String nameTranslationKey
) {
    public CharacterDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nameTranslationKey, "nameTranslationKey");

        if (nameTranslationKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Character translation key cannot be blank"
            );
        }
    }
}
