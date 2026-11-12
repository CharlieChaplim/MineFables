package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.character.kimiramatsu.KimiramatsuDefinition;
import net.minecraft.resources.ResourceLocation;

public final class CharacterBootstrap {

    private CharacterBootstrap() {
    }

    public static void registerAll() {
        // Mantemos a definicao propria do Kimiramatsu, que ja existe.
        CharacterRegistry.register(KimiramatsuDefinition.create());

        // Por enquanto, os demais so precisam de ID e nome traduzivel.
        register("tobi"); // Tobi
        register("ibi"); // Ibi
        register("guinevere"); // Guinevere
        register("nalik"); // Nalik
        register("emissor_do_caos"); // Emissor do Caos
        register("roy"); // Roy
        register("sumeragi"); // Sumeragi
        register("mordekaiser"); // Mordekaiser
        register("pato"); // Pato
        register("taty"); // Taty
        register("lori"); // Lori
        register("elias"); // Elias
        register("lastia"); // Lastia
        register("kuntiri"); // Kuntiri
        register("arisu"); // Arisu
        register("marina"); // Marina
        register("amnesia"); // Amnésia
        register("dante"); // Dante
        register("lucas"); // Lucas
        register("cross"); // Cross
        register("scintilla"); // Scintilla
        register("luke_distopia"); // Luke - Distopia
        register("sakuya"); // Sakuya
        register("melly"); // Melly
        register("unmei"); // Unmei
        register("madara"); // Madara
        register("o_carinha_foice"); // O carinha Foice
        register("shinichi"); // Shinichi
        register("hayato"); // Hayato
        register("kaito"); // Kaito
        register("shion"); // Shion
        register("nihil"); // Nihil
        register("akayuki"); // Akayuki
        register("temaiel"); // Temaiel
        register("kalista"); // Kalista
        register("junin"); // Junin
        register("william"); // William
        register("rem"); // Rem
        register("luke_primeira_gen"); // Luke - Primeira Gen
        register("padre_joao"); // Padre João
        register("chain"); // Chain
        register("lass"); // Lass
        register("chuai_xeng"); // Chuai Xeng
        register("yin"); // Yin
        register("ainz"); // Ainz
        register("luck"); // Luck
        register("brak"); // Brak
        register("outis"); // Outis
        register("draconir"); // Draconir
        register("ryuuko"); // Ryuuko
        register("padre_marcelo_rossi"); // Padre Marcelo Rossi
        register("rei_helstea"); // Rei Helstea
        register("asphodel"); // Asphodel
        register("tatsu"); // Tatsu
        register("wayne"); // Wayne
        register("bort"); // Bort
        register("rochelle"); // Rochelle
        register("ruth"); // Ruth
        register("pwie"); // Pwie
        register("betty"); // Betty

    }

    private static void register(String path) {
        CharacterRegistry.register(new CharacterDefinition(
                ResourceLocation.fromNamespaceAndPath(MineFables.MODID, path),
                "character.minefables." + path
        ));
    }
}
