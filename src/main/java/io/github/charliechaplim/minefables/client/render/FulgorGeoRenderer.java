package io.github.charliechaplim.minefables.client.render;

import io.github.charliechaplim.minefables.item.FulgorItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Renderiza a geometria completa da Fulgor quando o item esta na mao. */
public final class FulgorGeoRenderer extends GeoItemRenderer<FulgorItem> {
    public FulgorGeoRenderer() {
        super(new FulgorGeoModel());
    }
}
