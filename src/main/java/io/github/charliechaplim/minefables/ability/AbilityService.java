package io.github.charliechaplim.minefables.ability;

import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import io.github.charliechaplim.minefables.william.WilliamKit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Servidor determina modo, nivel e slot; nao confia no cliente. */
public final class AbilityService {
    private AbilityService() {}

    public static void use(ServerPlayer player, int index, boolean requestedUltimate) {
        var selected = PlayerCharacterService.getSelected(player);
        if (selected.isEmpty()) return;

        var id = selected.get().id();
        boolean isWilliam = id.equals(WilliamKit.ID);
        boolean actualUltimate = isWilliam && WilliamKit.isUltimateActive(player);
        // Nao pode interpretar um R normal como Overdrive se o pacote chegar atrasado.
        if (actualUltimate != requestedUltimate) {
            if (isWilliam) WilliamKit.sync(player);
            return;
        }
        int level = isWilliam ? WilliamKit.level(player) : Integer.MAX_VALUE;
        List<AbilityDefinition> abilities = AbilityRegistry.forCharacter(id, level, actualUltimate);
        if (index < 0 || index >= abilities.size()) return;

        AbilityDefinition ability = abilities.get(index);
        if (ability.effect().activate(player) && !isWilliam) {
            // William produz mensagens especificas, nao sobrescreve com nome generico.
            player.displayClientMessage(Component.translatable(ability.nameTranslationKey()), true);
        }
    }
}
