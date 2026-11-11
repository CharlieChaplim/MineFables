package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.data.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class PlayerCharacterService {

    private PlayerCharacterService() {
    }

    public static boolean select(ServerPlayer player, ResourceLocation characterId) {
        if (CharacterRegistry.get(characterId).isEmpty()) {
            return false;
        }

        player.setData(ModAttachments.SELECTED_CHARACTER, characterId.toString());
        return true;
    }

    public static Optional<CharacterDefinition> getSelected(ServerPlayer player) {
        String savedId = player.getData(ModAttachments.SELECTED_CHARACTER);

        if (savedId.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation characterId = ResourceLocation.tryParse(savedId);
        if (characterId == null) {
            return Optional.empty();
        }

        return CharacterRegistry.get(characterId);
    }
}
