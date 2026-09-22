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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Repère de nivellement mural : petite plaque de 6x8x2 pixels fixée sur la face
 * d'un bloc solide, comme un bouton. Modèle repris du mod Fabric bornedexmc.
 */
public class RepereNivellementBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RepereNivellementBlock> CODEC = simpleCodec(RepereNivellementBlock::new);

    // Formes pour chaque orientation (plaque de 2 pixels d'épaisseur collée au mur)
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0, 4.0, 14.0, 11.0, 12.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0, 4.0, 0.0, 11.0, 12.0, 2.0);
    private static final VoxelShape WEST_SHAPE = Block.box(14.0, 4.0, 5.0, 16.0, 12.0, 11.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 4.0, 5.0, 2.0, 12.0, 11.0);

    public RepereNivellementBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

        // Orientation face au joueur, si un bloc solide est derrière pour s'y fixer
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (state.canSurvive(level, pos)) {
            return state;
        }

        // Sinon, essayer les autres orientations horizontales
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            state = this.defaultBlockState().setValue(FACING, dir);
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
        // Le repère tombe si le bloc support disparaît
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
