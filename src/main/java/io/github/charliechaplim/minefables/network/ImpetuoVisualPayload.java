package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Sincroniza apenas o indicador visual do anel para observadores. */
public record ImpetuoVisualPayload(UUID playerId, boolean armed) implements CustomPacketPayload {
    public static final Type<ImpetuoVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "impetuo_visual"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImpetuoVisualPayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.playerId());
                buf.writeBoolean(packet.armed());
            },
            buf -> new ImpetuoVisualPayload(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
