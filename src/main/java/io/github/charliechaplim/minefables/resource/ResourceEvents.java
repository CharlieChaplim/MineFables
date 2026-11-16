package io.github.charliechaplim.minefables.resource;

import net.minecraft.server.level.ServerPlayer;
import io.github.charliechaplim.minefables.william.WilliamKit;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Regras iniciais de regeneracao, dano e combate. */
public final class ResourceEvents {
    private static final Map<UUID, Long> LAST_COMBAT_TICK = new HashMap<>();
    private static final long FIGHT_WINDOW_TICKS = 200;       // 10s apos o ultimo confronto
    private static final long STACK_DECAY_DELAY = 1_200;     // 60s sem combate

    private ResourceEvents() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceService.ensure(player);
            ResourceService.sync(player);
        }
    }

    /** Limpa o tempo de combate ao reiniciar a Fabula de debug. */
    public static void clearCombatState(ServerPlayer player) {
        LAST_COMBAT_TICK.remove(player.getUUID());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_COMBAT_TICK.remove(event.getEntity().getUUID());
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_COMBAT_TICK.remove(player.getUUID());
            ResourceService.ensure(player);
            ResourceService.set(player, ResourceType.STACK, 0);
            ResourceService.sync(player);
        }
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ResourceService.sync(player);
    }

    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        float effectiveHeal = Math.min(event.getAmount(), player.getMaxHealth() - player.getHealth());
        if (effectiveHeal >= 1.0f) {
            ResourceService.add(player, ResourceType.BLOOD, (long) Math.floor(effectiveHeal));
        }
    }

    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        Entity source = event.getSource().getEntity();
        if (source == null || source == event.getEntity()) return;

        if (source instanceof ServerPlayer attacker) markAndReward(attacker);
        if (event.getEntity() instanceof ServerPlayer victim) markAndReward(victim);
    }

    private static void markAndReward(ServerPlayer player) {
        long now = player.serverLevel().getServer().overworld().getGameTime();
        LAST_COMBAT_TICK.put(player.getUUID(), now);
        ResourceService.add(player, ResourceType.STACK, 1); // Valor provisorio por golpe
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (ResourceService.types(player).isEmpty()) return;

        long now = player.serverLevel().getServer().overworld().getGameTime();
        if (now % 20 == 0) {
            // William nao recupera Estamina enquanto mantem a Ressonancia.
            if (!WilliamKit.isResonating(player))
                ResourceService.add(player, ResourceType.STAMINA, 10); // 10/s provisorio
            ResourceService.add(player, ResourceType.MANA, 2);     // 2/s provisorio
            if (player.level().isDay()) ResourceService.add(player, ResourceType.FAITH, 2);
        }
        // 1000 pontos em 300 segundos = 1 a cada 6 ticks (20 ticks/s).
        if (now % 6 == 0) ResourceService.add(player, ResourceType.CURSED, 1);

        if (ResourceService.types(player).contains(ResourceType.STACK)) {
            Long last = LAST_COMBAT_TICK.get(player.getUUID());
            if (last != null && now - last <= FIGHT_WINDOW_TICKS && now % 60 == 0) {
                ResourceService.add(player, ResourceType.STACK, 1);
            }
            if (last != null && now - last >= STACK_DECAY_DELAY && now % 10 == 0) {
                ResourceService.trySpend(player, ResourceType.STACK, 1);
            }
        }
    }
}
