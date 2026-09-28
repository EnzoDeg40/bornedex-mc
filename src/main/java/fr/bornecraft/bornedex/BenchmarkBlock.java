package fr.bornecraft.bornedex;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Repère de nivellement (levelling benchmark) : plaque de 8x9x2 pixels fixée sur la face
 * d'un bloc solide, uniquement sur un mur (jamais au sol ni au plafond).
 * Peut être immergée (waterlogged) comme les escaliers.
 */
public class BenchmarkBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, EntityBlock {
    public static final MapCodec<BenchmarkBlock> CODEC = simpleCodec(BenchmarkBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Formes alignées sur le modèle (x 4..12, y 4..13, plaque de 2 pixels collée au mur)
    private static final VoxelShape NORTH_SHAPE = Block.box(4.0, 4.0, 14.0, 12.0, 13.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4.0, 4.0, 0.0, 12.0, 13.0, 2.0);
    private static final VoxelShape WEST_SHAPE = Block.box(14.0, 4.0, 4.0, 16.0, 13.0, 12.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 4.0, 4.0, 2.0, 13.0, 12.0);

    public BenchmarkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;

        // Face cliquée en priorité, si c'est un mur
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal()) {
            BlockState state = this.defaultBlockState().setValue(FACING, clicked).setValue(WATERLOGGED, waterlogged);
            if (state.canSurvive(level, pos)) {
                return state;
            }
        }

        // Sinon, le mur le plus proche de la direction du regard
        for (Direction dir : context.getNearestLookingDirections()) {
            if (!dir.getAxis().isHorizontal()) {
                continue;
            }
            BlockState state = this.defaultBlockState().setValue(FACING, dir.getOpposite()).setValue(WATERLOGGED, waterlogged);
            if (state.canSurvive(level, pos)) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(direction.getOpposite());
        return level.getBlockState(attachedPos).isFaceSturdy(level, attachedPos, direction);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // Le repère tombe si le bloc support disparaît
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BenchmarkBlockEntity(pos, state);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
