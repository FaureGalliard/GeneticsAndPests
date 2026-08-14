package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.CropGenes;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GeneticsAndPests.MODID);

    /** Per-chunk genome storage for every tracked plant standing in that chunk. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CropGenes>> CROP_GENES =
            ATTACHMENT_TYPES.register("crop_genes", () -> AttachmentType
                    .builder(CropGenes::new)
                    .serialize(CropGenes.CODEC, genes -> !genes.isEmpty())
                    // Synced so the client can name a plant's disease in the debug screen and to
                    // HUD mods; without this the infection exists only on the server.
                    .sync(CropGenes.STREAM_CODEC)
                    .build());

    private ModAttachments() {}
}
