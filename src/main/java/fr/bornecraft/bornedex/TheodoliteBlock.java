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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Théodolite : instrument sur trépied posé au sol. Se comporte comme une torche :
 * traversable, doit reposer sur un bloc solide, cassé (et droppé) par l'eau.
 * <p>
 * Clic droit : vise le repère de nivellement non gravé le plus proche (rayon
 * {@link #RANGE}, en ligne de vue), tire un laser de particules jusqu'à lui et grave sa cote.
 */
public class TheodoliteBlock extends Block {
    public static final MapCodec<TheodoliteBlock> CODEC = simpleCodec(TheodoliteBlock::new);

    /** Portée de visée, en blocs. */
    public static final int RANGE = 10;
    /** Espacement des particules du laser : 3 par bloc. */
    private static final double LASER_STEP = 1.0 / 3.0;
    /** Hauteur de la lunette au-dessus du sol (le modèle monte de 16 à 20 px). */
    private static final double LENS_HEIGHT = 18.0 / 16.0;
    /** Distance du centre du bloc à la face avant de la plaque du repère (plaque de 2 px collée au mur). */
    private static final double PLATE_OFFSET = 6.0 / 16.0;

    // Trépied + lunette (le modèle monte jusqu'à 20 pixels)
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
            Block.box(6.0, 16.0, 2.0, 10.0, 20.0, 14.0));

    public TheodoliteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Le théodolite tombe si le bloc dessous disparaît
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        List<BenchmarkBlockEntity> candidates = findUnsurveyedBenchmarks(serverLevel, pos);
        if (candidates.isEmpty()) {
            boolean anyInRange = !findBenchmarks(serverLevel, pos, false).isEmpty();
            String key = anyInRange ? "message.bornedex.theodolite.all_levelled"
                                    : "message.bornedex.theodolite.no_benchmark";
            player.displayClientMessage(Component.translatable(key, RANGE), true);
            return InteractionResult.CONSUME;
        }

        Vec3 lens = Vec3.atLowerCornerOf(pos).add(0.5, LENS_HEIGHT, 0.5);
        for (BenchmarkBlockEntity benchmark : candidates) {
            Vec3 plate = plateCenter(benchmark);
            if (!hasLineOfSight(serverLevel, lens, plate, benchmark.getBlockPos())) {
                continue;
            }
            fireLaser(serverLevel, lens, plate);
            benchmark.survey();
            serverLevel.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            serverLevel.playSound(null, benchmark.getBlockPos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.translatable("message.bornedex.theodolite.obstructed"), true);
        return InteractionResult.CONSUME;
    }

    /** Repères non gravés à portée, du plus proche au plus éloigné. */
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

    /** Centre de la face avant de la plaque (là où la cote est gravée). */
    private static Vec3 plateCenter(BenchmarkBlockEntity benchmark) {
        Direction facing = benchmark.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        // La plaque est collée au mur, côté opposé à FACING ; sa face avant regarde vers FACING.
        Vec3 center = Vec3.atCenterOf(benchmark.getBlockPos()).add(0.0, 0.5 / 16.0, 0.0);
        return center.subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(PLATE_OFFSET));
    }

    /** Vrai si rien de solide ne s'interpose entre la lunette et la plaque (l'eau ne gêne pas). */
    private static boolean hasLineOfSight(ServerLevel level, Vec3 from, Vec3 to, BlockPos target) {
        BlockHitResult result = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS || result.getBlockPos().equals(target);
    }

    /** Ligne de particules END_ROD de la lunette à la plaque, 3 par bloc. */
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
