package fr.bornecraft.bornedex;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity sans donnée : sert uniquement de support au renderer qui grave
 * la cote (altitude Y) sur la plaque du repère.
 */
public class BenchmarkBlockEntity extends BlockEntity {
    public BenchmarkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BENCHMARK.get(), pos, state);
    }

    /** Cote gravée sur la plaque : altitude du bloc par rapport au niveau de la mer (63 en surface). */
    public int getElevation() {
        int seaLevel = this.level != null ? this.level.getSeaLevel() : 63;
        return this.worldPosition.getY() - seaLevel;
    }
}
