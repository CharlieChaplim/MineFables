package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.character.kimiramatsu.KimiramatsuDefinition;
import net.minecraft.resources.ResourceLocation;

/** Elenco definido na lista revisada. Cada entrada e uma identidade unica. */
public final class CharacterBootstrap {

    private CharacterBootstrap() {}

    public static void registerAll() {
        CharacterRegistry.register(KimiramatsuDefinition.create());

        // Nove personagens ativos nesta etapa de desenvolvimento.
        register("william");
        register("lass");
        register("kalista");
        register("lastia");
        register("nalik");
        register("asphodel");
        register("roy");
        register("pellokia");
        // Personagem tecnico, usado apenas para testar sistemas.
        register("teste"); // Teste (desenvolvimento)
    }

    private static void register(String path) {
        CharacterRegistry.register(new CharacterDefinition(
                ResourceLocation.fromNamespaceAndPath(MineFables.MODID, path),
                "character.minefables." + path
        ));
    }
}
