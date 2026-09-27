package fr.bornecraft.bornedex;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Bornedex.MOD_ID)
public class Bornedex {
    public static final String MOD_ID = "bornedex";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BORNEDEX_TAB = CREATIVE_TABS.register("bornedex",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bornedex"))
                    .icon(() -> new ItemStack(ModBlocks.BENCHMARK_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.BENCHMARK_ITEM.get());
                        output.accept(ModBlocks.THEODOLITE_ITEM.get());
                    })
                    .build());

    public Bornedex(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        // STARTUP config is loaded on registration, so it is readable right below
        modContainer.registerConfig(ModConfig.Type.STARTUP, BornedexConfig.SPEC);

        // Default FTB quests (installed once into config/ftbquests/quests)
        if (ModList.get().isLoaded("ftbquests")) {
            QuestInstaller.install();
        }
    }
}
