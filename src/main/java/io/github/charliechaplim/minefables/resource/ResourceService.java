package io.github.charliechaplim.minefables.resource;

import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import io.github.charliechaplim.minefables.data.ModAttachments;
import io.github.charliechaplim.minefables.network.ResourceNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Fonte de verdade no servidor. Nunca confia nos valores enviados pelo cliente. */
public final class ResourceService {
    private ResourceService() {}

    public static Set<ResourceType> types(ServerPlayer player) {
        ResourceLocation characterId = PlayerCharacterService.getSelected(player)
                .map(def -> def.id()).orElse(null);
        return CharacterResourceProfiles.forCharacter(characterId);
    }

    public static void ensure(ServerPlayer player) {
        if (PlayerCharacterService.getSelected(player).isEmpty()) return;
        if (player.hasData(ModAttachments.RESOURCE_VALUES.get())) return;

        Map<String, Long> initial = new HashMap<>();
        for (ResourceType type : types(player)) initial.put(type.id(), type.initialValue());
        player.setData(ModAttachments.RESOURCE_VALUES, Map.copyOf(initial));
    }

    public static long get(ServerPlayer player, ResourceType type) {
        if (!types(player).contains(type)) return 0;
        ensure(player);
        return player.getData(ModAttachments.RESOURCE_VALUES).getOrDefault(type.id(), type.initialValue());
    }

    public static long capacity(ServerPlayer player, ResourceType type) {
        return types(player).contains(type) ? type.baseCapacity() : 0;
    }

    public static boolean set(ServerPlayer player, ResourceType type, long amount) {
        if (!types(player).contains(type)) return false;
        ensure(player);
        long clamped = Math.max(0, Math.min(capacity(player, type), amount));
        if (get(player, type) == clamped) return true;

        Map<String, Long> values = new HashMap<>(player.getData(ModAttachments.RESOURCE_VALUES));
        values.put(type.id(), clamped);
        player.setData(ModAttachments.RESOURCE_VALUES, Map.copyOf(values));
        sync(player);
        return true;
    }

    public static boolean add(ServerPlayer player, ResourceType type, long amount) {
        if (!types(player).contains(type)) return false;
        long current = get(player, type);
        long positive = Math.max(0, amount);
        return set(player, type, current > Long.MAX_VALUE - positive ? Long.MAX_VALUE : current + positive);
    }

    /** Tenta gastar sem ultrapassar zero. Sangue->vida sera implementado na aula de habilidades. */
    public static boolean trySpend(ServerPlayer player, ResourceType type, long amount) {
        if (amount < 0 || !types(player).contains(type) || get(player, type) < amount) return false;
        return set(player, type, get(player, type) - amount);
    }

    public static void sync(ServerPlayer player) {
        if (PlayerCharacterService.getSelected(player).isEmpty()) return;
        ensure(player);
        String selectedId = PlayerCharacterService.getSelected(player).orElseThrow().id().toString();
        ResourceNetworking.send(player, selectedId, player.getData(ModAttachments.RESOURCE_VALUES));
    }
}
