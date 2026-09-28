package fr.bornecraft.bornedex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Mémorise l'orientation de la lunette (azimut et inclinaison, convention vanilla :
 * yaw 0 = sud, pitch positif = vers le bas). Elle pivote d'un coup vers le dernier repère
 * visé et garde cette position : on voit que l'instrument a servi. Le bloc cassé repart à zéro.
 */
public class TheodoliteBlockEntity extends BlockEntity {
    private static final String TAG_YAW = "Yaw";
    private static final String TAG_PITCH = "Pitch";

    private float yaw;
    private float pitch;

    public TheodoliteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THEODOLITE.get(), pos, state);
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    /** Pointe la lunette de {@code from} vers {@code to} et pousse la mise à jour aux clients. */
    public void aimAt(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        this.yaw = (float) Mth.wrapDegrees(Math.toDegrees(Mth.atan2(-d.x, d.z)));
        this.pitch = (float) Math.toDegrees(-Mth.atan2(d.y, horizontal));
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(TAG_YAW, this.yaw);
        tag.putFloat(TAG_PITCH, this.pitch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.yaw = tag.getFloat(TAG_YAW);
        this.pitch = tag.getFloat(TAG_PITCH);
    }

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
