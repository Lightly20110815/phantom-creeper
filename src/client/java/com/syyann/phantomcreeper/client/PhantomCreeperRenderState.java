package com.syyann.phantomcreeper.client;

// 1.21.2+：幻翼的渲染状态，额外带上引信进度（渲染时不能再直接读实体，数据要先拷贝到渲染状态里）
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.PhantomRenderState;

public class PhantomCreeperRenderState extends PhantomRenderState {
    // 引信进度，0 = 未点燃，>= 1 = 即将爆炸
    public float swelling;
}
*///?}
