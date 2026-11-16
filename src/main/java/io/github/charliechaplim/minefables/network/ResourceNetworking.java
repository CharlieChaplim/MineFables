package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.client.ClientResourceState;
import io.github.charliechaplim.minefables.ability.AbilityService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;

/** Registra a sincronizacao servidor -> cliente e o uso de habilidade cliente -> servidor. */
public final class ResourceNetworking {
    private ResourceNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                ResourceSyncPayload.TYPE,
                ResourceSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientResourceState.accept(payload)
        );
        registrar.playToServer(
                UseAbilityPayload.TYPE,
                UseAbilityPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        AbilityService.use(player, payload.abilityIndex());
                    }
                }
        );
    }

    public static void send(ServerPlayer player, String selectedId, Map<String, Long> values) {
        PacketDistributor.sendToPlayer(player, new ResourceSyncPayload(selectedId, Map.copyOf(values)));
    }
}
