package fr.bornecraft.bornedex.client;

import fr.bornecraft.bornedex.Bornedex;
import fr.bornecraft.bornedex.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Bornedex.MOD_ID, value = Dist.CLIENT)
public final class BornedexClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BENCHMARK.get(), BenchmarkRenderer::new);
    }

    private BornedexClient() {}
}
