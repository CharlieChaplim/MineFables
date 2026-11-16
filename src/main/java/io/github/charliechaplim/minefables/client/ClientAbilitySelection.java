package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.ability.AbilityRegistry;
import io.github.charliechaplim.minefables.william.WilliamKit;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** A troca de personagem OU modo reinicia o indice para evitar slots invalidos. */
public final class ClientAbilitySelection {
    private static int index;
    private static String previousCharacter = "";
    private static boolean previousMode;

    private ClientAbilitySelection() {}

    public static void clear() {
        index = 0;
        previousCharacter = "";
        previousMode = false;
    }

    public static boolean ultimateMode() {
        ResourceLocation character = currentCharacter();
        return WilliamKit.ID.equals(character) && ClientWilliamState.ultimate();
    }

    public static int selectedIndex() {
        refreshState();
        if (count() == 0) return 0;
        index = Math.min(index, count() - 1);
        return index;
    }

    public static int count() {
        ResourceLocation character = currentCharacter();
        if (character == null) return 0;
        int level = WilliamKit.ID.equals(character) ? ClientWilliamState.level() : Integer.MAX_VALUE;
        return AbilityRegistry.forCharacter(character, level, ultimateMode()).size();
    }

    public static void cycle(int direction) {
        refreshState();
        int count = count();
        if (count > 0) index = Math.floorMod(index + direction, count);
    }

    public static ResourceLocation currentCharacter() {
        return ResourceLocation.tryParse(ClientResourceState.characterId());
    }

    private static void refreshState() {
        String current = ClientResourceState.characterId();
        boolean mode = ultimateMode();
        if (!Objects.equals(current, previousCharacter) || mode != previousMode) {
            previousCharacter = current;
            previousMode = mode;
            index = 0;
        }
    }
}
