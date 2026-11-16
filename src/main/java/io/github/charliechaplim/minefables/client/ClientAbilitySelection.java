package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.ability.AbilityRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Indice de HUD local; nao e autoridade sobre o personagem ou a Mana. */
public final class ClientAbilitySelection {
    private static int index = 0;
    private static String previousCharacter = "";

    private ClientAbilitySelection() {}

    public static void clear() {
        index = 0;
        previousCharacter = "";
    }

    public static int selectedIndex() {
        refreshCharacter();
        return index;
    }

    public static int count() {
        var id = currentCharacter();
        return id == null ? 0 : AbilityRegistry.forCharacter(id).size();
    }

    public static void cycle(int direction) {
        refreshCharacter();
        int count = count();
        if (count > 0) index = Math.floorMod(index + direction, count);
    }

    public static ResourceLocation currentCharacter() {
        return ResourceLocation.tryParse(ClientResourceState.characterId());
    }

    private static void refreshCharacter() {
        String current = ClientResourceState.characterId();
        if (!Objects.equals(current, previousCharacter)) {
            previousCharacter = current;
            index = 0;
        }
    }
}
