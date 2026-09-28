package fr.bornecraft.bornedex;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
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
                    .icon(() -> new ItemStack(ModBlocks.REPERE_NIVELLEMENT_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.REPERE_NIVELLEMENT_ITEM.get());
                        output.accept(ModBlocks.THEODOLITE_ITEM.get());
                    })
                    .build());

    public Bornedex(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        // Quêtes FTB par défaut (copiées dans config/ftbquests/quests si absentes)
        if (ModList.get().isLoaded("ftbquests")) {
            QuestInstaller.installIfMissing();
        }
    }
}
