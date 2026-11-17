package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Somente aparência. O servidor decide quando a ultimate está ativa. */
public record DragonInstallStatePayload(UUID playerId, boolean active) implements CustomPacketPayload {
    public static final Type<DragonInstallStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "dragon_install_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DragonInstallStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.playerId());
                buf.writeBoolean(payload.active());
            },
            buf -> new DragonInstallStatePayload(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
