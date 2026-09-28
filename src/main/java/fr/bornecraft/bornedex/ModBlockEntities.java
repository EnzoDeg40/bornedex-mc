package fr.bornecraft.bornedex;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bornedex.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BenchmarkBlockEntity>> BENCHMARK =
            BLOCK_ENTITIES.register("benchmark",
                    () -> BlockEntityType.Builder.of(BenchmarkBlockEntity::new, ModBlocks.BENCHMARK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TheodoliteBlockEntity>> THEODOLITE =
            BLOCK_ENTITIES.register("theodolite",
                    () -> BlockEntityType.Builder.of(TheodoliteBlockEntity::new, ModBlocks.THEODOLITE.get()).build(null));

    private ModBlockEntities() {}
}
