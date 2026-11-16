package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.network.ResourceSyncPayload;
import java.util.Map;

/** Cache somente para renderizacao. Os dados oficiais ficam no servidor. */
public final class ClientResourceState {
    private static String characterId = "";
    private static Map<String, Long> values = Map.of();

    private ClientResourceState() {}

    public static void accept(ResourceSyncPayload payload) {
        characterId = payload.characterId();
        values = Map.copyOf(payload.values());
    }

    public static String characterId() { return characterId; }
    public static Map<String, Long> values() { return values; }
    public static void clear() { characterId = ""; values = Map.of(); }
}
