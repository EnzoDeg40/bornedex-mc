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
 * Grave la cote sur la plaque centrale du repère (zone creuse de 6x4 pixels,
 * x 5..11, y 6..10, en retrait à z = 15 sur le modèle orienté nord).
 */
public class BenchmarkRenderer implements BlockEntityRenderer<BenchmarkBlockEntity> {
    /** Hauteur de police 9 px * échelle = 3 pixels de texture. */
    private static final float SCALE = 3.0f / 16.0f / 9.0f;
    private static final int TEXT_COLOR = FastColor.ARGB32.color(255, 210, 210, 210);

    private final Font font;

    public BenchmarkRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    /**
     * Le moins ASCII fait 5 px de large dans la police vanilla ; le trait d'union U+2010
     * n'en fait que 3, à la même hauteur, ce qui garde la cote négative compacte.
     */
    private static String formatElevation(int elevation) {
        return elevation < 0 ? "\u2010" + (-elevation) : Integer.toString(elevation);
    }

    @Override
    public void render(BenchmarkBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Plaque vierge tant qu'aucun théodolite n'a gravé la cote
        if (!blockEntity.hasElevation()) {
            return;
        }
        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        String text = formatElevation(blockEntity.getElevation());

        poseStack.pushPose();
        // Centre du bloc, puis rotation selon l'orientation (même recette que SignRenderer)
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // Après rotation, le +z local pointe vers le joueur (face avant) et la plaque est collée au mur
        // derrière, en -z local. Le fond de la plaque est à 15/16 du mur, soit z local = -(15/16 - 0.5) ;
        // on avance d'un poil pour éviter le z-fighting.
        poseStack.translate(0.0, 0.0, -(15.0 / 16.0 - 0.5) + 0.002);
        poseStack.scale(SCALE, -SCALE, SCALE);

        float x = -this.font.width(text) / 2.0f;
        float y = -this.font.lineHeight / 2.0f + 1.0f;
        this.font.drawInBatch(text, x, y, TEXT_COLOR, false, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        poseStack.popPose();
    }
}
