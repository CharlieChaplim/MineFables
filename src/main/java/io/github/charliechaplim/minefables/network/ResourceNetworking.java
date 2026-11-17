package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.client.ClientResourceState;
import io.github.charliechaplim.minefables.client.ClientWilliamState;
import io.github.charliechaplim.minefables.client.ClientDragonInstallState;
import io.github.charliechaplim.minefables.client.ClientImpetuoState;
import io.github.charliechaplim.minefables.ability.AbilityService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;

/** Sincroniza recursos + status William e usa indices verificados no servidor. */
public final class ResourceNetworking {
    private ResourceNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // Protocolo 4: visuais Dragon Install e anel do Impetuo.
        var registrar = event.registrar("4");
        registrar.playToClient(ResourceSyncPayload.TYPE, ResourceSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientResourceState.accept(payload));
        registrar.playToClient(WilliamStatusPayload.TYPE, WilliamStatusPayload.STREAM_CODEC,
                (payload, context) -> ClientWilliamState.accept(payload));
        registrar.playToClient(DragonInstallStatePayload.TYPE, DragonInstallStatePayload.STREAM_CODEC,
                (payload, context) -> ClientDragonInstallState.accept(payload));
        registrar.playToClient(ImpetuoVisualPayload.TYPE, ImpetuoVisualPayload.STREAM_CODEC,
                (payload, context) -> ClientImpetuoState.accept(payload.playerId(), payload.armed()));
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

    /** Visível por jogadores próximos, mas o custo/dano nunca depende desse pacote. */
    public static void sendImpetuo(ServerPlayer player, boolean armed) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ImpetuoVisualPayload(player.getUUID(), armed));
    }

    public static void sendImpetuoTo(ServerPlayer watcher, ServerPlayer target) {
        PacketDistributor.sendToPlayer(watcher,
                new ImpetuoVisualPayload(target.getUUID(), true));
    }

    /** Estado de armadura visual, transmitido apenas a quem acompanha o jogador. */
    public static void sendDragon(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new DragonInstallStatePayload(player.getUUID(), active));
    }

    /** Entrega o estado atual a clientes que comecam a enxergar um William ja transformado. */
    public static void sendDragonTo(ServerPlayer watcher, ServerPlayer target) {
        PacketDistributor.sendToPlayer(watcher,
                new DragonInstallStatePayload(target.getUUID(), true));
    }
}
