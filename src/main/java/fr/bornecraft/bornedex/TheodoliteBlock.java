package fr.bornecraft.bornedex;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Theodolite: tripod-mounted instrument placed on the ground. Behaves like a torch:
 * no collision, must rest on a solid block, broken (and dropped) by water.
 * <p>
 * Right click: targets the nearest unengraved levelling benchmark (within {@link #RANGE},
 * in line of sight), aims the scope at it, fires a particle laser and engraves its
 * elevation. The scope then keeps that orientation ({@link TheodoliteBlockEntity}).
 * <p>
 * A redstone signal does the same as a right click, once per rising edge.
 */
public class TheodoliteBlock extends Block implements EntityBlock {
    public static final MapCodec<TheodoliteBlock> CODEC = simpleCodec(TheodoliteBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** Aiming range, in blocks. */
    public static final int RANGE = 10;
    /** Laser particle spacing: 3 per block. */
    private static final double LASER_STEP = 1.0 / 3.0;
    /** Scope height above the ground (the model spans 16 to 20 px). */
    private static final double LENS_HEIGHT = 18.0 / 16.0;
    /** Distance from the block center to the benchmark plate's front face (2 px plate against the wall). */
    private static final double PLATE_OFFSET = 6.0 / 16.0;

    // Tripod + scope (the model goes up to 20 pixels)
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
            Block.box(6.0, 16.0, 2.0, 10.0, 20.0, 14.0));

    public TheodoliteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Placed against an active signal, it starts powered and waits for the next rising edge
        BlockState state = this.defaultBlockState()
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // The theodolite drops if the block below disappears
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        if (powered && level instanceof ServerLevel serverLevel) {
            survey(serverLevel, pos, null);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TheodoliteBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        survey(serverLevel, pos, player);
        return InteractionResult.CONSUME;
    }

    /**
     * Surveys the nearest unengraved benchmark in line of sight. {@code player} is the one
     * operating the theodolite, or null when it is triggered by redstone: nobody then gets
     * the failure messages nor the advancement.
     */
    private static void survey(ServerLevel serverLevel, BlockPos pos, @Nullable Player player) {
        List<BenchmarkBlockEntity> candidates = findUnsurveyedBenchmarks(serverLevel, pos);
        if (candidates.isEmpty()) {
            boolean anyInRange = !findBenchmarks(serverLevel, pos, false).isEmpty();
            String key = anyInRange ? "message.bornedex.theodolite.all_levelled"
                                    : "message.bornedex.theodolite.no_benchmark";
            notify(player, Component.translatable(key, RANGE));
            return;
        }

        Vec3 lens = Vec3.atLowerCornerOf(pos).add(0.5, LENS_HEIGHT, 0.5);
        for (BenchmarkBlockEntity benchmark : candidates) {
            Vec3 plate = plateCenter(benchmark);
            if (!hasLineOfSight(serverLevel, lens, plate, benchmark.getBlockPos())) {
                continue;
            }
            if (serverLevel.getBlockEntity(pos) instanceof TheodoliteBlockEntity theodolite) {
                theodolite.aimAt(lens, plate);
            }
            fireLaser(serverLevel, lens, plate);
            benchmark.survey();
            if (player != null) {
                ModAdvancements.award(player, ModAdvancements.FIRST_SURVEY);
            }
            serverLevel.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            serverLevel.playSound(null, benchmark.getBlockPos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
            return;
        }

        notify(player, Component.translatable("message.bornedex.theodolite.obstructed"));
    }

    private static void notify(@Nullable Player player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    /** Unengraved benchmarks in range, nearest first. */
    private static List<BenchmarkBlockEntity> findUnsurveyedBenchmarks(ServerLevel level, BlockPos origin) {
        return findBenchmarks(level, origin, true);
    }

    private static List<BenchmarkBlockEntity> findBenchmarks(ServerLevel level, BlockPos origin, boolean unsurveyedOnly) {
        List<BenchmarkBlockEntity> found = new ArrayList<>();
        int rangeSq = RANGE * RANGE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-RANGE, -RANGE, -RANGE), origin.offset(RANGE, RANGE, RANGE))) {
            if (p.distSqr(origin) > rangeSq || !level.getBlockState(p).is(ModBlocks.BENCHMARK.get())) {
                continue;
            }
            if (level.getBlockEntity(p) instanceof BenchmarkBlockEntity benchmark
                    && (!unsurveyedOnly || !benchmark.hasElevation())) {
                found.add(benchmark);
            }
        }
        found.sort(Comparator.comparingDouble(b -> b.getBlockPos().distSqr(origin)));
        return found;
    }

    /** Center of the plate's front face (where the elevation is engraved). */
    private static Vec3 plateCenter(BenchmarkBlockEntity benchmark) {
        Direction facing = benchmark.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        // The plate sits against the wall, opposite FACING; its front face looks toward FACING.
        Vec3 center = Vec3.atCenterOf(benchmark.getBlockPos()).add(0.0, 0.5 / 16.0, 0.0);
        return center.subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(PLATE_OFFSET));
    }

    /** True if nothing solid lies between the scope and the plate (water does not block). */
    private static boolean hasLineOfSight(ServerLevel level, Vec3 from, Vec3 to, BlockPos target) {
        BlockHitResult result = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS || result.getBlockPos().equals(target);
    }

    /** Line of END_ROD particles from the scope to the plate, 3 per block. */
    private static void fireLaser(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0e-3) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        for (double d = 0.0; d <= length; d += LASER_STEP) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
