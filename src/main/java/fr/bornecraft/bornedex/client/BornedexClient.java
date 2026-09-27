package fr.bornecraft.bornedex.client;

import fr.bornecraft.bornedex.Bornedex;
import fr.bornecraft.bornedex.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = Bornedex.MOD_ID, value = Dist.CLIENT)
public final class BornedexClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BENCHMARK.get(), BenchmarkRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.THEODOLITE.get(), TheodoliteRenderer::new);
    }

    /** The scope is not in the blockstate, so it is loaded as a standalone model. */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TheodoliteRenderer.HEAD_MODEL);
    }

    private BornedexClient() {}
}
