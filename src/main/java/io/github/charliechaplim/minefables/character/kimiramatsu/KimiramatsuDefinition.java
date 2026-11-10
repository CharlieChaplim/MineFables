
package io.github.charliechaplim.minefables.character.kimiramatsu;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.character.CharacterDefinition;

import net.minecraft.resources.ResourceLocation;

public final class KimiramatsuDefinition {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MineFables.MODID,
                    "kimiramatsu"
            );

    private KimiramatsuDefinition() {
    }

    public static CharacterDefinition create() {
        return new CharacterDefinition(
                ID,
                "character.minefables.kimiramatsu"
        );
    }
}
