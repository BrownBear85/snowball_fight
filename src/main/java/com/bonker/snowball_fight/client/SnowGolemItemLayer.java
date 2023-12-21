package com.bonker.snowball_fight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SnowGolemItemLayer extends RenderLayer<SnowGolem, SnowGolemModel<SnowGolem>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public SnowGolemItemLayer(SnowGolemRenderer pRenderer, ItemInHandRenderer pItemInHandRenderer) {
        super(pRenderer);
        this.itemInHandRenderer = pItemInHandRenderer;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SnowGolem entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean leftHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        ItemStack offHand = leftHanded ? entity.getOffhandItem() : entity.getMainHandItem();
        ItemStack mainHand = leftHanded ? entity.getMainHandItem() : entity.getOffhandItem();
        if (!offHand.isEmpty() || !mainHand.isEmpty()) {
            poseStack.pushPose();
            renderArmWithItem(entity, mainHand, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, packedLight);
            renderArmWithItem(entity, offHand, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }

    protected void renderArmWithItem(SnowGolem entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                     PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!stack.isEmpty()) {
            poseStack.pushPose();

            boolean leftHand = arm == HumanoidArm.LEFT;

            (leftHand ? getParentModel().leftArm : getParentModel().rightArm).translateAndRotate(poseStack);

            poseStack.translate(0.6, leftHand ? 0.025F : 0.05F, leftHand ? -0.3F :  0.3F);

            poseStack.mulPose(Axis.YP.rotationDegrees(leftHand ? 0 : 180));
            poseStack.mulPose(Axis.ZP.rotationDegrees(leftHand ? 90 : -110));


            itemInHandRenderer.renderItem(entity, stack, context, leftHand, poseStack, bufferSource, packedLight);

            poseStack.popPose();
        }
    }
}
