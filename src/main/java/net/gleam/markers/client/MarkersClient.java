package net.gleam.markers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.gleam.markers.Markers;
import net.gleam.markers.client.render.MarkerBlockEntityRenderer;
import net.minecraft.client.render.RenderLayer;

public class MarkersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(Markers.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        //Config?
    }
}
