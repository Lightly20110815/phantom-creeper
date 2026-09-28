package com.syyann.phantomcreeper.client;

// 1.21.2+ 的渲染器：在原版幻翼渲染器基础上，把幻翼抬到碰撞箱顶部，下面挂一只苦力怕，引信点燃时整只一起闪白。
// 数值和算法与 LegacyPhantomCreeperRenderer 相同，只是改成了新版的“渲染状态”写法。
//? if >=1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;

public class PhantomCreeperRenderer extends PhantomRenderer {
    // 幻翼模型整体上移的高度（格），给下面的苦力怕腾出位置
    private static final float MODEL_LIFT = 1.25f;
    // 俯仰旋转的支点高度（格），约为幻翼身体中心
    private static final float PITCH_PIVOT = 1.45f;

    public PhantomCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new HangingCreeperLayer(this, context.getModelSet()));
    }

    @Override
    public PhantomCreeperRenderState createRenderState() {
        return new PhantomCreeperRenderState();
    }

    @Override
    public void extractRenderState(Phantom entity, PhantomRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof PhantomCreeperEntity creeper && state instanceof PhantomCreeperRenderState creeperState) {
            creeperState.swelling = creeper.getClientFuseTime(partialTick);
        }
    }

    @Override
    protected void setupRotations(PhantomRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        // 原版绕脚底做俯仰，模型抬高后要改成绕幻翼身体旋转
        poseStack.translate(0.0f, PITCH_PIVOT, 0.0f);
        super.setupRotations(state, poseStack, bodyRot, scale);
        poseStack.translate(0.0f, -PITCH_PIVOT, 0.0f);
    }

    @Override
    protected void scale(PhantomRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        // 此时坐标系 y 轴朝下，负数表示往上
        poseStack.translate(0.0f, -MODEL_LIFT, 0.0f);
    }

    @Override
    protected float getWhiteOverlayProgress(PhantomRenderState state) {
        return state instanceof PhantomCreeperRenderState creeperState ? fuseFlash(creeperState.swelling) : 0.0f;
    }

    // 与原版 CreeperRenderer 相同的闪白节奏
    static float fuseFlash(float swelling) {
        if ((int) (swelling * 10.0f) % 2 == 0) {
            return 0.0f;
        }
        return Mth.clamp(swelling, 0.5f, 1.0f);
    }
}
*///?}
