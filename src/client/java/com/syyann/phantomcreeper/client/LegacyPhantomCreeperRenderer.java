package com.syyann.phantomcreeper.client;

// 1.21.1 及更早版本的渲染器（1.21.2 起原版渲染改为“渲染状态 + 提交”结构，见 PhantomCreeperRenderer）
//? if <1.21.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;

/**
 * 在原版幻翼渲染器基础上：把幻翼抬到碰撞箱顶部，下面挂一只苦力怕，
 * 引信点燃时整只一起闪白。
 */
public class LegacyPhantomCreeperRenderer extends PhantomRenderer {
    /** 幻翼模型整体上移的高度（格），给下面的苦力怕腾出位置 */
    private static final float MODEL_LIFT = 1.25f;
    /** 俯仰旋转的支点高度（格），约为幻翼身体中心 */
    private static final float PITCH_PIVOT = 1.45f;

    public LegacyPhantomCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new LegacyHangingCreeperLayer(this, context.getModelSet()));
    }

    // 原版绕脚底做俯仰，模型抬高后要改成绕幻翼身体旋转
    //? if >=1.20.5 {
    /*@Override
    protected void setupRotations(Phantom entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
        matrices.translate(0.0f, PITCH_PIVOT, 0.0f);
        super.setupRotations(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
        matrices.translate(0.0f, -PITCH_PIVOT, 0.0f);
    }
    *///?} else {
    @Override
    protected void setupRotations(Phantom entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
        matrices.translate(0.0f, PITCH_PIVOT, 0.0f);
        super.setupRotations(entity, matrices, animationProgress, bodyYaw, tickDelta);
        matrices.translate(0.0f, -PITCH_PIVOT, 0.0f);
    }
    //?}

    @Override
    protected void scale(Phantom entity, PoseStack matrices, float amount) {
        super.scale(entity, matrices, amount);
        // 此时坐标系 y 轴朝下，负数表示往上
        matrices.translate(0.0f, -MODEL_LIFT, 0.0f);
    }

    @Override
    protected float getWhiteOverlayProgress(Phantom entity, float tickDelta) {
        return entity instanceof PhantomCreeperEntity creeper ? fuseFlash(creeper, tickDelta) : 0.0f;
    }

    /** 与原版 CreeperEntityRenderer 相同的闪白节奏 */
    static float fuseFlash(PhantomCreeperEntity entity, float tickDelta) {
        float fuse = entity.getClientFuseTime(tickDelta);
        if ((int) (fuse * 10.0f) % 2 == 0) {
            return 0.0f;
        }
        return Mth.clamp(fuse, 0.5f, 1.0f);
    }
}
//?}
