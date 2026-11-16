package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Modo enviado exclusivamente como verificacao contra pacotes de um modo antigo. */
public record UseAbilityPayload(int abilityIndex, boolean ultimateMode) implements CustomPacketPayload {
    public static final Type<UseAbilityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "use_ability")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, UseAbilityPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.abilityIndex());
                buffer.writeBoolean(payload.ultimateMode());
            },
            buffer -> new UseAbilityPayload(buffer.readVarInt(), buffer.readBoolean())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
