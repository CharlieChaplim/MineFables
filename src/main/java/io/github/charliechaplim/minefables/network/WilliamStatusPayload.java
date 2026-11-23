package io.github.charliechaplim.minefables.network;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cooldowns enviados pelo servidor em ticks, associados aos slots de cada modo. */
public record WilliamStatusPayload(int level, boolean ultimate, int resonanceTier,
                                   boolean impetuoPrepared, int remainingSeconds,
                                   int temperature, int[] normalCooldowns, int[] ultimateCooldowns)
        implements CustomPacketPayload {
    public static final Type<WilliamStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MineFables.MODID, "william_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WilliamStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.level());buf.writeBoolean(payload.ultimate());
                buf.writeVarInt(payload.resonanceTier());buf.writeBoolean(payload.impetuoPrepared());
                buf.writeVarInt(payload.remainingSeconds());buf.writeVarInt(payload.temperature());
                for (int i=0;i<6;i++)buf.writeVarInt(payload.normalCooldowns()[i]);
                for (int i=0;i<4;i++)buf.writeVarInt(payload.ultimateCooldowns()[i]);
            }, buf -> {
                int level=buf.readVarInt();boolean ultimate=buf.readBoolean();
                int tier=buf.readVarInt();boolean prep=buf.readBoolean();
                int seconds=buf.readVarInt(), heat=buf.readVarInt();
                int[] normal=new int[6], over=new int[4];
                for(int i=0;i<6;i++)normal[i]=buf.readVarInt();
                for(int i=0;i<4;i++)over[i]=buf.readVarInt();
                return new WilliamStatusPayload(level,ultimate,tier,prep,seconds,heat,normal,over);
            });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
