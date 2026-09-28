package fr.bornecraft.bornedex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.bornecraft.bornedex.Bornedex;
import fr.bornecraft.bornedex.TheodoliteBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Dessine la lunette du théodolite (modèle {@code theodolite_head}) pivotée selon
 * l'orientation mémorisée. Le trépied reste un modèle de bloc classique.
 */
public class TheodoliteRenderer implements BlockEntityRenderer<TheodoliteBlockEntity> {
    public static final ModelResourceLocation HEAD_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Bornedex.MOD_ID, "block/theodolite_head"));

    /** Pivot de la lunette : centre de l'élément (8, 18, 8) du modèle. */
    private static final float PIVOT_Y = 18.0f / 16.0f;

    private final BlockRenderDispatcher blockRenderer;

    public TheodoliteRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TheodoliteBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel head = Minecraft.getInstance().getModelManager().getModel(HEAD_MODEL);

        poseStack.pushPose();
        poseStack.translate(0.5f, PIVOT_Y, 0.5f);
        // Même convention que les entités : la lunette (axe +z du modèle) regarde vers (yaw, pitch)
        poseStack.mulPose(Axis.YP.rotationDegrees(-blockEntity.getYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(blockEntity.getPitch()));
        poseStack.translate(-0.5f, -PIVOT_Y, -0.5f);

        this.blockRenderer.getModelRenderer().renderModel(poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()), blockEntity.getBlockState(), head,
                1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
