package com.syyann.phantomcreeper.client;

// 1.21.1 及更早版本的渲染器（1.21.2 起原版渲染改为“渲染状态 + 提交”结构，见 PhantomCreeperRenderer）
//? if <1.21.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;

/** 在幻翼身体下方渲染一只吊着的苦力怕（直接用原版苦力怕的模型和贴图） */
public class LegacyHangingCreeperLayer extends RenderLayer<Phantom, PhantomModel<Phantom>> {
    //? if >=1.21 {
    /*private static final ResourceLocation CREEPER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png");
    *///?} else {
    private static final ResourceLocation CREEPER_TEXTURE = new ResourceLocation("textures/entity/creeper/creeper.png");
    //?}
    /** 苦力怕缩放：原版苦力怕略大于幻翼身体，缩小一点更像被“叼着” */
    private static final float CREEPER_SCALE = 0.75f;
    /** 挂点：幻翼身体正下方（模型坐标，单位格，y 轴朝下） */
    private static final float ATTACH_Y = 1.5f / 16.0f;

    private final CreeperModel<Phantom> creeperModel;

    public LegacyHangingCreeperLayer(RenderLayerParent<Phantom, PhantomModel<Phantom>> context, EntityModelSet loader) {
        super(context);
        this.creeperModel = new CreeperModel<>(loader.bakeLayer(ModelLayers.CREEPER));
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, Phantom entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(entity instanceof PhantomCreeperEntity creeper)) {
            return;
        }
        matrices.pushPose();
        matrices.translate(0.0f, ATTACH_Y, 0.0f);
        // 抵消幻翼的俯仰，让苦力怕始终竖直下垂，再加一点前后摆动
        matrices.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(Mth.sin(animationProgress * 0.15f) * 6.0f));

        // 引信膨胀效果，算法同原版 CreeperEntityRenderer#scale
        float fuse = creeper.getClientFuseTime(tickDelta);
        float wobble = 1.0f + Mth.sin(fuse * 100.0f) * fuse * 0.01f;
        float swell = Mth.clamp(fuse, 0.0f, 1.0f);
        swell *= swell;
        swell *= swell;
        float horizontal = (1.0f + swell * 0.4f) * wobble * CREEPER_SCALE;
        float vertical = (1.0f + swell * 0.1f) / wobble * CREEPER_SCALE;
        matrices.scale(horizontal, vertical, horizontal);
        // 苦力怕模型头顶在 y = -2 像素处，下移使头顶贴住挂点
        matrices.translate(0.0f, 2.0f / 16.0f, 0.0f);

        // 腿轻轻晃动，像悬空乱蹬
        this.creeperModel.setupAnim(entity, animationProgress * 0.25f, 0.25f, animationProgress, 0.0f, 0.0f);
        VertexConsumer vertices = vertexConsumers.getBuffer(this.creeperModel.renderType(CREEPER_TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, LegacyPhantomCreeperRenderer.fuseFlash(creeper, tickDelta));
        //? if >=1.21 {
        /*this.creeperModel.renderToBuffer(matrices, vertices, light, overlay);
        *///?} else {
        this.creeperModel.renderToBuffer(matrices, vertices, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        //?}
        matrices.popPose();
    }
}
//?}
