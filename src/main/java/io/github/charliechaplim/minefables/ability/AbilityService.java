package io.github.charliechaplim.minefables.ability;

import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Recebe a solicitacao do cliente e valida tudo no servidor. */
public final class AbilityService {
    private AbilityService() {}

    public static void use(ServerPlayer player, int index) {
        var selected = PlayerCharacterService.getSelected(player);
        if (selected.isEmpty()) return;

        List<AbilityDefinition> abilities = AbilityRegistry.forCharacter(selected.get().id());
        if (index < 0 || index >= abilities.size()) return;

        AbilityDefinition ability = abilities.get(index);
        if (ability.effect().activate(player)) {
            player.displayClientMessage(Component.translatable(ability.nameTranslationKey()), true);
        }
    }
}
