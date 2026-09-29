package fr.bornecraft.bornedex;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Bornedex.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Bornedex.MOD_ID);

    /** Levelling benchmark: stone geodetic plate fixed to a wall, waterloggable. */
    public static final DeferredBlock<BenchmarkBlock> BENCHMARK = BLOCKS.registerBlock("benchmark",
            BenchmarkBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> BENCHMARK_ITEM =
            ITEMS.registerSimpleBlockItem("benchmark", BENCHMARK);

    /**
     * Theodolite: placed on the ground like a torch, no collision, instantly broken
     * by hand, washed away (and dropped) by water.
     */
    public static final DeferredBlock<TheodoliteBlock> THEODOLITE = BLOCKS.registerBlock("theodolite",
            TheodoliteBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> THEODOLITE_ITEM =
            ITEMS.registerSimpleBlockItem("theodolite", THEODOLITE);

    private ModBlocks() {}
}
