package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.character.kimiramatsu.KimiramatsuDefinition;
import net.minecraft.resources.ResourceLocation;

/** Elenco definido na lista revisada. Cada entrada e uma identidade unica. */
public final class CharacterBootstrap {

    private CharacterBootstrap() {}

    public static void registerAll() {
        CharacterRegistry.register(KimiramatsuDefinition.create());

        register("ainz"); // Ainz
        register("akayuki"); // Akayuki
        register("amnesia"); // Amnésia
        register("arisu"); // Arisu
        register("asphodel"); // Asphodel
        register("betty"); // Betty
        register("bort"); // Bort
        register("brak"); // Brak
        register("chain"); // Chain
        register("chuai_xeng"); // Chuai Xeng
        register("cross"); // Cross
        register("dante"); // Dante
        register("draconir"); // Draconir
        register("elias"); // Elias
        register("guinevere"); // Guinevere
        register("hayato"); // Hayato
        register("ibi"); // Ibi
        register("junin"); // Junin
        register("kaito"); // Kaito
        register("kalista"); // Kalista
        register("kuntiri"); // Kuntiri
        register("lass"); // Lass
        register("lastia"); // Lastia
        register("lori"); // Lori
        register("lucas"); // Lucas
        register("luck"); // Luck
        register("luke"); // Luke
        register("paracelcius"); // Paracelcius
        register("madara"); // Madara
        register("marina"); // Marina
        register("melly"); // Melly
        register("mordekaiser"); // Mordekaiser
        register("nalik"); // Nalik
        register("nihil"); // Nihil
        register("daniel"); // Daniel
        register("outis"); // Outis
        register("padre_joao"); // Padre João
        register("padre_marcelo_rossi"); // Padre Marcelo Rossi
        register("pato"); // Pato
        register("pwie"); // Pwie
        register("rei_helstea"); // Rei Helstea
        register("sophia"); // Sophia
        register("ashley"); // Ashley
        register("roy"); // Roy
        register("ruth"); // Ruth
        register("ryuuko"); // Ryuuko
        register("sakuya"); // Sakuya
        register("scintilla"); // Scintilla
        register("shinichi"); // Shinichi
        register("shion"); // Shion
        register("sumeragi"); // Sumeragi
        register("tatsu"); // Tatsu
        register("taty"); // Taty
        register("tamaiel"); // Tamaiel
        register("tobi"); // Tobi
        register("unmei"); // Unmei
        register("wayne"); // Wayne
        register("william"); // William

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
