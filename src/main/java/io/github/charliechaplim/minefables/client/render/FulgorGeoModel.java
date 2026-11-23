package io.github.charliechaplim.minefables.client.render;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.item.FulgorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modelo GeckoLib original do Blockbench, sem a conversao intermediaria para OBJ. */
public final class FulgorGeoModel extends GeoModel<FulgorItem> {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "geo/item/fulgor.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "textures/item/fulgor_geo.png");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(
            MineFables.MODID, "animations/fulgor.animation.json");

    @Override
    public ResourceLocation getModelResource(FulgorItem item) { return GEO; }

    @Override
    public ResourceLocation getTextureResource(FulgorItem item) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(FulgorItem item) { return ANIM; }
}
