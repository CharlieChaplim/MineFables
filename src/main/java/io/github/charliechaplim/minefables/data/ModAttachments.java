package io.github.charliechaplim.minefables.data;

import com.mojang.serialization.Codec;
import io.github.charliechaplim.minefables.MineFables;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

/** Identidade do jogador e suas reservas persistentes. */
public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MineFables.MODID);

    public static final Supplier<AttachmentType<String>> SELECTED_CHARACTER =
            ATTACHMENTS.register("selected_character", () ->
                    AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<Map<String, Long>>> RESOURCE_VALUES =
            ATTACHMENTS.register("resource_values", () ->
                    AttachmentType.builder(() -> Map.<String, Long>of())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG))
                            .copyOnDeath()
                            .build());

    private ModAttachments() {}

    public static void register(IEventBus modEventBus) { ATTACHMENTS.register(modEventBus); }
}
