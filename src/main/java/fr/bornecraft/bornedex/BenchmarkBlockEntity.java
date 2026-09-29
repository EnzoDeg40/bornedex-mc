package fr.bornecraft.bornedex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Repère de nivellement : la cote (altitude par rapport au niveau de la mer) n'est pas
 * connue à la pose. Elle est gravée par un théodolite à portée ({@link TheodoliteBlock}),
 * puis synchronisée au client pour que le renderer l'affiche.
 */
public class BenchmarkBlockEntity extends BlockEntity {
    private static final String TAG_ELEVATION = "Elevation";

    @Nullable
    private Integer elevation;

    public BenchmarkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BENCHMARK.get(), pos, state);
    }

    /** Vrai une fois la cote gravée par un théodolite. */
    public boolean hasElevation() {
        return this.elevation != null;
    }

    /** Cote gravée sur la plaque ; n'appeler que si {@link #hasElevation()}. */
    public int getElevation() {
        return this.elevation != null ? this.elevation : 0;
    }

    /**
     * Grave la cote courante du bloc (Y - niveau de la mer du générateur de la dimension)
     * et pousse la mise à jour aux clients. Côté serveur uniquement.
     */
    public void survey() {
        if (!(this.level instanceof ServerLevel serverLevel) || this.hasElevation()) {
            return;
        }
        int seaLevel = serverLevel.getChunkSource().getGenerator().getSeaLevel();
        this.elevation = this.worldPosition.getY() - seaLevel;
        this.setChanged();
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.elevation != null) {
            tag.putInt(TAG_ELEVATION, this.elevation);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.elevation = tag.contains(TAG_ELEVATION) ? tag.getInt(TAG_ELEVATION) : null;
    }

    // --- Synchronisation client (chargement de chunk + mise à jour ponctuelle) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
