package com.syyann.phantomcreeper.client;

import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PhantomEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** 在幻翼身体下方渲染一只吊着的苦力怕（直接用原版苦力怕的模型和贴图） */
public class HangingCreeperFeatureRenderer extends FeatureRenderer<PhantomEntity, PhantomEntityModel<PhantomEntity>> {
    private static final Identifier CREEPER_TEXTURE = new Identifier("textures/entity/creeper/creeper.png");
    /** 苦力怕缩放：原版苦力怕略大于幻翼身体，缩小一点更像被“叼着” */
    private static final float CREEPER_SCALE = 0.75f;
    /** 挂点：幻翼身体正下方（模型坐标，单位格，y 轴朝下） */
    private static final float ATTACH_Y = 1.5f / 16.0f;

    private final CreeperEntityModel<PhantomEntity> creeperModel;

    public HangingCreeperFeatureRenderer(FeatureRendererContext<PhantomEntity, PhantomEntityModel<PhantomEntity>> context, EntityModelLoader loader) {
        super(context);
        this.creeperModel = new CreeperEntityModel<>(loader.getModelPart(EntityModelLayers.CREEPER));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PhantomEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(entity instanceof PhantomCreeperEntity creeper)) {
            return;
        }
        matrices.push();
        matrices.translate(0.0f, ATTACH_Y, 0.0f);
        // 抵消幻翼的俯仰，让苦力怕始终竖直下垂，再加一点前后摆动
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.sin(animationProgress * 0.15f) * 6.0f));

        // 引信膨胀效果，算法同原版 CreeperEntityRenderer#scale
        float fuse = creeper.getClientFuseTime(tickDelta);
        float wobble = 1.0f + MathHelper.sin(fuse * 100.0f) * fuse * 0.01f;
        float swell = MathHelper.clamp(fuse, 0.0f, 1.0f);
        swell *= swell;
        swell *= swell;
        float horizontal = (1.0f + swell * 0.4f) * wobble * CREEPER_SCALE;
        float vertical = (1.0f + swell * 0.1f) / wobble * CREEPER_SCALE;
        matrices.scale(horizontal, vertical, horizontal);
        // 苦力怕模型头顶在 y = -2 像素处，下移使头顶贴住挂点
        matrices.translate(0.0f, 2.0f / 16.0f, 0.0f);

        // 腿轻轻晃动，像悬空乱蹬
        this.creeperModel.setAngles(entity, animationProgress * 0.25f, 0.25f, animationProgress, 0.0f, 0.0f);
        VertexConsumer vertices = vertexConsumers.getBuffer(this.creeperModel.getLayer(CREEPER_TEXTURE));
        int overlay = LivingEntityRenderer.getOverlay(entity, PhantomCreeperRenderer.fuseFlash(creeper, tickDelta));
        this.creeperModel.render(matrices, vertices, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        matrices.pop();
    }
}
