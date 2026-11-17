package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.network.DragonInstallStatePayload;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cache apenas visual por UUID, incluindo jogadores remotos em multiplayer. */
public final class ClientDragonInstallState {
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private ClientDragonInstallState() {}

    public static void accept(DragonInstallStatePayload payload) {
        if (!payload.active()) {
            ACTIVE.remove(payload.playerId());
        } else {
            var level = Minecraft.getInstance().level;
            // Atualização a cada 10 ticks. A validade evita a armadura presa após desync.
            ACTIVE.put(payload.playerId(), level == null ? 0L : level.getGameTime());
        }
    }

    public static boolean isActive(UUID id) {
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        Long last = ACTIVE.get(id);
        if (last == null) return false;
        long age = level.getGameTime() - last;
        if (age < 0 || age > 80) {
            ACTIVE.remove(id);
            return false;
        }
        return true;
    }

    public static void clear() { ACTIVE.clear(); }
}
