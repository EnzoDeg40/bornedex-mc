package fr.bornecraft.bornedex;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Brushing a benchmark erases its elevation, so it can be surveyed again.
 * <p>
 * The vanilla brush already plays its animation, dust particles and sound on any block,
 * but only acts on suspicious blocks. This hooks into the same use tick and, on each
 * brush stroke (same timing as {@link BrushItem#onUseTick}), brushes the targeted benchmark.
 */
@EventBusSubscriber(modid = Bornedex.MOD_ID)
public final class BenchmarkBrushing {
    /** BrushItem strokes every 10 ticks, on the 5th tick of each period. */
    private static final int STROKE_PERIOD = 10;
    private static final int STROKE_OFFSET = 5;

    private BenchmarkBrushing() {}

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof BrushItem)
                || !(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int elapsed = stack.getUseDuration(player) - event.getDuration() + 1;
        if (elapsed % STROKE_PERIOD != STROKE_OFFSET) {
            return;
        }
        // Same targeting as BrushItem#calculateHitResult
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player,
                entity -> !entity.isSpectator() && entity.isPickable(), player.blockInteractionRange());
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !(level.getBlockEntity(blockHit.getBlockPos()) instanceof BenchmarkBlockEntity benchmark)
                || !benchmark.brush(level.getGameTime())) {
            return;
        }
        EquipmentSlot slot = stack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, player, slot);
        level.playSound(null, blockHit.getBlockPos(), SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 1.0f, 0.7f);
        ModAdvancements.award(player, ModAdvancements.CLEAN_SLATE);
    }
}
