package fr.bornecraft.bornedex;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;

import java.util.Objects;

/**
 * Taking a renamed benchmark out of an anvil grants the "Christening" advancement.
 * The name itself is carried by the item's {@code custom_name} component ({@link BenchmarkBlockEntity}).
 */
@EventBusSubscriber(modid = Bornedex.MOD_ID)
public final class BenchmarkNaming {
    private BenchmarkNaming() {}

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack output = event.getOutput();
        Component name = output.get(DataComponents.CUSTOM_NAME);
        // A real rename: the output has a name, different from the input's
        if (!output.is(ModBlocks.BENCHMARK_ITEM.get()) || name == null
                || Objects.equals(name, event.getLeft().get(DataComponents.CUSTOM_NAME))) {
            return;
        }
        ModAdvancements.award(event.getEntity(), ModAdvancements.CHRISTENING);
    }
}
