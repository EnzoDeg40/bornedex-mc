package fr.bornecraft.bornedex;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
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

    /** Repère de nivellement : plaque géodésique fixée au mur. */
    public static final DeferredBlock<RepereNivellementBlock> REPERE_NIVELLEMENT = BLOCKS.registerBlock("repere_nivellement",
            RepereNivellementBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> REPERE_NIVELLEMENT_ITEM =
            ITEMS.registerSimpleBlockItem("repere_nivellement", REPERE_NIVELLEMENT);

    /**
     * Théodolite : bloc au sol se comportant comme les hautes herbes,
     * sans collision, cassé instantanément à la main et emporté par l'eau.
     */
    public static final DeferredBlock<Block> THEODOLITE = BLOCKS.registerSimpleBlock("theodolite",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> THEODOLITE_ITEM =
            ITEMS.registerSimpleBlockItem("theodolite", THEODOLITE);

    private ModBlocks() {}
}
