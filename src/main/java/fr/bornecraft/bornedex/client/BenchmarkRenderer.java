package fr.bornecraft.bornedex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.bornecraft.bornedex.BenchmarkBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Engraves the elevation on the benchmark's central plate (6x4 pixel recess,
 * x 5..11, y 6..10, inset at z = 15 on the north-facing model).
 */
public class BenchmarkRenderer implements BlockEntityRenderer<BenchmarkBlockEntity> {
    /** Font height 9 px * scale = 3 texture pixels. */
    private static final float SCALE = 3.0f / 16.0f / 9.0f;
    private static final int TEXT_COLOR = FastColor.ARGB32.color(255, 210, 210, 210);

    private final Font font;

    public BenchmarkRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    /**
     * The ASCII minus is 5 px wide in the vanilla font; the U+2010 hyphen is only 3,
     * at the same height, which keeps negative elevations compact.
     */
    private static String formatElevation(int elevation) {
        return elevation < 0 ? "\u2010" + (-elevation) : Integer.toString(elevation);
    }

    @Override
    public void render(BenchmarkBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Blank plate until a theodolite has engraved the elevation
        if (!blockEntity.hasElevation()) {
            return;
        }
        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        String text = formatElevation(blockEntity.getElevation());

        poseStack.pushPose();
        // Block center, then rotate by facing (same recipe as SignRenderer)
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // After rotation, local +z points toward the player (front face) and the plate sits against
        // the wall behind, at local -z. The plate's recess is at 15/16 from the wall, i.e. local
        // z = -(15/16 - 0.5); nudge forward slightly to avoid z-fighting.
        poseStack.translate(0.0, 0.0, -(15.0 / 16.0 - 0.5) + 0.002);
        poseStack.scale(SCALE, -SCALE, SCALE);

        float x = -this.font.width(text) / 2.0f;
        float y = -this.font.lineHeight / 2.0f + 1.0f;
        this.font.drawInBatch(text, x, y, TEXT_COLOR, false, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        poseStack.popPose();
    }
}
