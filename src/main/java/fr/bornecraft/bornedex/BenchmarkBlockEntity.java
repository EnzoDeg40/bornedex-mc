package fr.bornecraft.bornedex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Levelling benchmark: its elevation (height above sea level) is unknown when placed.
 * It is engraved by a theodolite in range ({@link TheodoliteBlock}), then synced to the
 * client so the renderer can display it. Brushing the plate erases it ({@link BenchmarkBrushing}).
 * <p>
 * A benchmark item renamed in an anvil gives its name to the placed benchmark (shown when the
 * player looks at it). Breaking it drops a benchmark with the same name, but never the elevation.
 */
public class BenchmarkBlockEntity extends BlockEntity {
    private static final String TAG_ELEVATION = "Elevation";
    private static final String TAG_CUSTOM_NAME = "CustomName";
    /** Brush strokes (one every 10 ticks) needed to erase the elevation: about 2.5 seconds. */
    private static final int REQUIRED_BRUSHES = 5;
    /** Brushing progress is lost if the player stops for this many ticks. */
    private static final long BRUSH_RESET_TICKS = 40L;

    @Nullable
    private Integer elevation;
    @Nullable
    private Component customName;
    // Brushing progress, transient like suspicious sand's
    private int brushCount;
    private long brushCountResetsAtTick;

    public BenchmarkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BENCHMARK.get(), pos, state);
    }

    /** True once the elevation has been engraved by a theodolite. */
    public boolean hasElevation() {
        return this.elevation != null;
    }

    /** Elevation engraved on the plate; only call if {@link #hasElevation()}. */
    public int getElevation() {
        return this.elevation != null ? this.elevation : 0;
    }

    /**
     * Engraves the block's current elevation (Y minus the dimension generator's sea level)
     * and pushes the update to clients. Server side only.
     */
    public void survey() {
        if (!(this.level instanceof ServerLevel serverLevel) || this.hasElevation()) {
            return;
        }
        int seaLevel = serverLevel.getChunkSource().getGenerator().getSeaLevel();
        this.elevation = this.worldPosition.getY() - seaLevel;
        this.sync();
    }

    /** Name given in an anvil, or null if the benchmark has none (the default). */
    @Nullable
    public Component getCustomName() {
        return this.customName;
    }

    /**
     * One brush stroke on the plate. Returns true when the stroke erased the elevation.
     * Server side only.
     */
    public boolean brush(long gameTime) {
        if (!this.hasElevation()) {
            return false;
        }
        if (gameTime >= this.brushCountResetsAtTick) {
            this.brushCount = 0;
        }
        this.brushCountResetsAtTick = gameTime + BRUSH_RESET_TICKS;
        if (++this.brushCount < REQUIRED_BRUSHES) {
            return false;
        }
        this.brushCount = 0;
        this.elevation = null;
        this.sync();
        return true;
    }

    /** Marks the block entity dirty and pushes its data to clients. */
    private void sync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.elevation != null) {
            tag.putInt(TAG_ELEVATION, this.elevation);
        }
        if (this.customName != null) {
            tag.putString(TAG_CUSTOM_NAME, Component.Serializer.toJson(this.customName, registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.elevation = tag.contains(TAG_ELEVATION) ? tag.getInt(TAG_ELEVATION) : null;
        this.customName = tag.contains(TAG_CUSTOM_NAME, Tag.TAG_STRING)
                ? parseCustomNameSafe(tag.getString(TAG_CUSTOM_NAME), registries)
                : null;
    }

    // --- Item components (same as banners): the name goes from the item to the block and back ---

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        this.customName = componentInput.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.customName);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(TAG_CUSTOM_NAME);
    }

    // --- Client sync (chunk load + one-off update) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * NeoForge ignores empty update tags by default, but an empty tag is meaningful here:
     * a benchmark without a name whose elevation was just erased.
     */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        this.loadWithComponents(packet.getTag(), registries);
    }
}
