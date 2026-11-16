package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente informa somente o indice selecionado; servidor decide se pode usar. */
public record UseAbilityPayload(int abilityIndex) implements CustomPacketPayload {
    public static final Type<UseAbilityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "use_ability")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UseAbilityPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.abilityIndex()),
            buffer -> new UseAbilityPayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
