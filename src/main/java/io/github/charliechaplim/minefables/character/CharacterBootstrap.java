
package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.character.kimiramatsu.KimiramatsuDefinition;

public final class CharacterBootstrap {

    private CharacterBootstrap() {
    }

    public static void registerAll() {
        CharacterRegistry.register(
                KimiramatsuDefinition.create()
        );
    }
}
