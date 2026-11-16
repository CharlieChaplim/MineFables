package io.github.charliechaplim.minefables.character;

import io.github.charliechaplim.minefables.data.ModAttachments;
import io.github.charliechaplim.minefables.william.WilliamKit;
import io.github.charliechaplim.minefables.resource.ResourceService;
import io.github.charliechaplim.minefables.resource.ResourceEvents;
import io.github.charliechaplim.minefables.network.ResourceNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Map;

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
        ResourceService.ensure(player);
        ResourceService.sync(player);
        WilliamKit.sync(player);
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

    /**
     * Apaga SOMENTE os dados de Fabula implementados ate a Aula 4.
     * Chamado exclusivamente pela Maca de Debugger no servidor.
     * O inventario, XP, posicao e status vanilla nao sao alterados.
     */
    public static boolean resetForDebug(ServerPlayer player) {
        boolean hadCharacter = player.hasData(ModAttachments.SELECTED_CHARACTER.get())
                && !player.getData(ModAttachments.SELECTED_CHARACTER).isBlank();
        boolean hadResources = player.hasData(ModAttachments.RESOURCE_VALUES.get());

        WilliamKit.reset(player);
        player.removeData(ModAttachments.SELECTED_CHARACTER.get());
        player.removeData(ModAttachments.RESOURCE_VALUES.get());
        ResourceEvents.clearCombatState(player);

        // ResourceService.sync() NAO envia pacotes quando nao ha personagem.
        // Enviamos um estado explicitamente vazio para limpar a HUD do cliente.
        ResourceNetworking.send(player, "", Map.of());
        WilliamKit.sync(player);
        return hadCharacter || hadResources;
    }

    // Sera utilizado por habilidades, passivas, equipamentos e Ultimates.
    public static boolean isUsing(ServerPlayer player, ResourceLocation characterId) {
        return getSelected(player)
                .map(character -> character.id().equals(characterId))
                .orElse(false);
    }
}
