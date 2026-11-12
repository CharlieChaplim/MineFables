package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.data.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Escolha de UMA Fabula por jogador, validada somente no servidor. */
public final class PlayerCharacterService {

    public enum SelectionResult {
        SELECTED,
        ALREADY_USING,
        ALREADY_HAS_CHARACTER,
        UNKNOWN_CHARACTER
    }

    private PlayerCharacterService() {
    }

    public static SelectionResult select(ServerPlayer player, ResourceLocation requestedId) {
        if (CharacterRegistry.get(requestedId).isEmpty()) {
            return SelectionResult.UNKNOWN_CHARACTER;
        }

        // Nunca sobrescreve uma identidade que ja foi escolhida.
        // Isso impede que alguem que possui A adquira B pelo comando.
        String savedId = player.getData(ModAttachments.SELECTED_CHARACTER);
        if (!savedId.isBlank()) {
            return savedId.equals(requestedId.toString())
                    ? SelectionResult.ALREADY_USING
                    : SelectionResult.ALREADY_HAS_CHARACTER;
        }

        player.setData(ModAttachments.SELECTED_CHARACTER, requestedId.toString());
        return SelectionResult.SELECTED;
    }

    public static Optional<CharacterDefinition> getSelected(ServerPlayer player) {
        String savedId = player.getData(ModAttachments.SELECTED_CHARACTER);
        if (savedId.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(savedId);
        return id == null ? Optional.empty() : CharacterRegistry.get(id);
    }

    // Sera utilizado por habilidades, passivas, equipamentos e Ultimates.
    public static boolean isUsing(ServerPlayer player, ResourceLocation characterId) {
        return getSelected(player)
                .map(character -> character.id().equals(characterId))
                .orElse(false);
    }
}
