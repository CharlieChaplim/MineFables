package io.github.charliechaplim.minefables.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Só visual: a verdade sobre o Impétuo permanece no servidor. */
public final class ClientImpetuoState {
    private static final Set<UUID> ARMED_PLAYERS = new HashSet<>();

    private ClientImpetuoState() {}

    public static void accept(UUID playerId, boolean armed) {
        if (armed) ARMED_PLAYERS.add(playerId);
        else ARMED_PLAYERS.remove(playerId);
    }

    public static boolean isArmed(Player player) {
        var self = Minecraft.getInstance().player;
        if (self != null && self.getUUID().equals(player.getUUID())) {
            return ClientWilliamState.prepared();
        }
        return ARMED_PLAYERS.contains(player.getUUID());
    }

    public static void clear() {
        ARMED_PLAYERS.clear();
    }
}
