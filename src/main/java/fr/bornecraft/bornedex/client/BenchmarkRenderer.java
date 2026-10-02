package fr.bornecraft.bornedex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.bornecraft.bornedex.BenchmarkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

/**
 * Engraves the elevation on the benchmark's central plate (6x4 pixel recess,
 * x 5..11, y 6..10, inset at z = 15 on the north-facing model), and shows the
 * benchmark's name above it, like a name tag, while the player is looking at it.
 */
public class BenchmarkRenderer implements BlockEntityRenderer<BenchmarkBlockEntity> {
    /** Font height 9 px * scale = 3 texture pixels. */
    private static final float SCALE = 3.0f / 16.0f / 9.0f;
    private static final int TEXT_COLOR = FastColor.ARGB32.color(255, 210, 210, 210);

    /** Same scale as entity name tags. */
    private static final float NAME_SCALE = 0.025f;
    /** Name tag height above the block's bottom. */
    private static final double NAME_HEIGHT = 1.0;
    /** Horizontal offset from the block center to the plate center (2 px plate against the wall). */
    private static final double PLATE_OFFSET = 7.0 / 16.0;
    /** Name tag colors, as in EntityRenderer#renderNameTag: translucent see-through pass, then opaque. */
    private static final int NAME_SEE_THROUGH_COLOR = 0x20FFFFFF;
    private static final int NAME_COLOR = 0xFFFFFFFF;

    private final Font font;
    private final EntityRenderDispatcher entityRenderDispatcher;

    public BenchmarkRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.entityRenderDispatcher = context.getEntityRenderer();
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
        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        Component name = blockEntity.getCustomName();
        if (name != null && isLookedAt(blockEntity)) {
            this.renderName(name, facing, poseStack, bufferSource, packedLight);
        }
        // Blank plate until a theodolite has engraved the elevation
        if (blockEntity.hasElevation()) {
            this.renderElevation(blockEntity.getElevation(), facing, poseStack, bufferSource, packedLight);
        }
    }

    private static boolean isLookedAt(BenchmarkBlockEntity blockEntity) {
        Minecraft minecraft = Minecraft.getInstance();
        return !minecraft.options.hideGui
                && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(blockEntity.getBlockPos());
    }

    /** Billboarded label above the plate, drawn like EntityRenderer#renderNameTag. */
    private void renderName(Component name, Direction facing, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // The plate sits against the wall, opposite FACING
        poseStack.translate(0.5 - facing.getStepX() * PLATE_OFFSET, NAME_HEIGHT, 0.5 - facing.getStepZ() * PLATE_OFFSET);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(NAME_SCALE, -NAME_SCALE, NAME_SCALE);

        Matrix4f pose = poseStack.last().pose();
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
        float x = -this.font.width(name) / 2.0f;
        this.font.drawInBatch(name, x, 0.0f, NAME_SEE_THROUGH_COLOR, false, pose, bufferSource,
                Font.DisplayMode.SEE_THROUGH, background, packedLight);
        this.font.drawInBatch(name, x, 0.0f, NAME_COLOR, false, pose, bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private void renderElevation(int elevation, Direction facing, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight) {
        String text = formatElevation(elevation);

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
