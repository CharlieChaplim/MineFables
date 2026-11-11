package io.github.charliechaplim.minefables.data;

import com.mojang.serialization.Codec;
import io.github.charliechaplim.minefables.MineFables;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MineFables.MODID);

    public static final Supplier<AttachmentType<String>> SELECTED_CHARACTER =
            ATTACHMENTS.register("selected_character", () ->
                    AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .copyOnDeath()
                            .build()
            );

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
