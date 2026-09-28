package com.syyann.phantomcreeper.client;

// 1.21.2+：在幻翼身体下方渲染一只吊着的苦力怕（直接用原版苦力怕的模型和贴图），写法参考原版 ParrotOnShoulderLayer
//? if >=1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HangingCreeperLayer extends RenderLayer<PhantomRenderState, PhantomModel> {
    private static final ResourceLocation CREEPER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png");
    // 苦力怕缩放：原版苦力怕略大于幻翼身体，缩小一点更像被“叼着”
    private static final float CREEPER_SCALE = 0.75f;
    // 挂点：幻翼身体正下方（模型坐标，单位格，y 轴朝下）
    private static final float ATTACH_Y = 1.5f / 16.0f;

    private final CreeperModel creeperModel;

    public HangingCreeperLayer(RenderLayerParent<PhantomRenderState, PhantomModel> parent, EntityModelSet models) {
        super(parent);
        this.creeperModel = new CreeperModel(models.bakeLayer(ModelLayers.CREEPER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, PhantomRenderState state, float yRot, float xRot) {
        if (!(state instanceof PhantomCreeperRenderState creeperState) || state.isInvisible) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0f, ATTACH_Y, 0.0f);
        // 抵消幻翼的俯仰，让苦力怕始终竖直下垂，再加一点前后摆动
        float swing = Mth.sin(state.ageInTicks * 0.15f) * 6.0f;
        //? if >=26.1 {
        /^poseStack.rotateDegrees(Axis.XP, state.xRot);
        poseStack.rotateDegrees(Axis.XP, swing);
        ^///?} else {
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(swing));
        //?}

        // 引信膨胀效果，算法同原版 CreeperRenderer#scale
        float fuse = creeperState.swelling;
        float wobble = 1.0f + Mth.sin(fuse * 100.0f) * fuse * 0.01f;
        float swell = Mth.clamp(fuse, 0.0f, 1.0f);
        swell *= swell;
        swell *= swell;
        float horizontal = (1.0f + swell * 0.4f) * wobble * CREEPER_SCALE;
        float vertical = (1.0f + swell * 0.1f) / wobble * CREEPER_SCALE;
        poseStack.scale(horizontal, vertical, horizontal);
        // 苦力怕模型头顶在 y = -2 像素处，下移使头顶贴住挂点
        poseStack.translate(0.0f, 2.0f / 16.0f, 0.0f);

        // 提交是延迟渲染的，每帧新建一个苦力怕渲染状态；腿轻轻晃动，像悬空乱蹬
        CreeperRenderState creeper = new CreeperRenderState();
        creeper.ageInTicks = state.ageInTicks;
        creeper.walkAnimationPos = state.ageInTicks * 0.25f;
        creeper.walkAnimationSpeed = 0.25f;
        int overlay = LivingEntityRenderer.getOverlayCoords(state, PhantomCreeperRenderer.fuseFlash(fuse));
        //? if >=26.1 {
        /^collector.submitModel(this.creeperModel, creeper, poseStack, this.creeperModel.renderType(CREEPER_TEXTURE),
                light, overlay, state.outlineColor);
        ^///?} else {
        collector.submitModel(this.creeperModel, creeper, poseStack, this.creeperModel.renderType(CREEPER_TEXTURE),
                light, overlay, state.outlineColor, null);
        //?}
        poseStack.popPose();
    }
}
*///?}
