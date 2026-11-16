package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Pacote servidor -> cliente com o estado atual das barras. */
public record ResourceSyncPayload(String characterId, Map<String, Long> values) implements CustomPacketPayload {
    public static final Type<ResourceSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "resource_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeUtf(data.characterId(), 100);
                buf.writeVarInt(data.values().size());
                data.values().forEach((key, value) -> {
                    buf.writeUtf(key, 32);
                    buf.writeVarLong(value);
                });
            },
            buf -> {
                String characterId = buf.readUtf(100);
                int count = buf.readVarInt();
                if (count < 0 || count > 7) throw new IllegalArgumentException("Quantidade invalida de recursos: " + count);
                Map<String, Long> values = new HashMap<>();
                for (int i = 0; i < count; i++) values.put(buf.readUtf(32), buf.readVarLong());
                return new ResourceSyncPayload(characterId, Map.copyOf(values));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
