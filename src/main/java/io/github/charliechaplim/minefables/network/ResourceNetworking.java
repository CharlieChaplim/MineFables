package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.client.ClientResourceState;
import io.github.charliechaplim.minefables.client.ClientWilliamState;
import io.github.charliechaplim.minefables.ability.AbilityService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;

/** Sincroniza recursos + status William e usa indices verificados no servidor. */
public final class ResourceNetworking {
    private ResourceNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // Protocolo 2 inclui o bit de modo em UseAbilityPayload.
        var registrar = event.registrar("2");
        registrar.playToClient(ResourceSyncPayload.TYPE, ResourceSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientResourceState.accept(payload));
        registrar.playToClient(WilliamStatusPayload.TYPE, WilliamStatusPayload.STREAM_CODEC,
                (payload, context) -> ClientWilliamState.accept(payload));
        registrar.playToServer(UseAbilityPayload.TYPE, UseAbilityPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player)
                        AbilityService.use(player, payload.abilityIndex(), payload.ultimateMode());
                });
    }

    public static void send(ServerPlayer player, String selectedId, Map<String, Long> values) {
        PacketDistributor.sendToPlayer(player, new ResourceSyncPayload(selectedId, Map.copyOf(values)));
    }

    public static void sendWilliam(ServerPlayer player, WilliamStatusPayload status) {
        PacketDistributor.sendToPlayer(player, status);
    }
}
