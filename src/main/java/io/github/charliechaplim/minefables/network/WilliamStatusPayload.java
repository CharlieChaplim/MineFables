package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Estado da UI de William, calculado apenas no servidor. */
public record WilliamStatusPayload(int level, boolean ultimate, int resonanceTier,
                                   boolean impetuoPrepared, int remainingSeconds) implements CustomPacketPayload {
    public static final Type<WilliamStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "william_status")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, WilliamStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.level());
                buffer.writeBoolean(payload.ultimate());
                buffer.writeVarInt(payload.resonanceTier());
                buffer.writeBoolean(payload.impetuoPrepared());
                buffer.writeVarInt(payload.remainingSeconds());
            },
            buffer -> new WilliamStatusPayload(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
