package com.syyann.phantomcreeper.entity;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
//? if >=1.21.6 {
/*import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?} else {
import net.minecraft.nbt.CompoundTag;
//?}
//? if >=1.21.11 {
/*import net.minecraft.world.attribute.EnvironmentAttributes;
*///?}
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.syyann.phantomcreeper.ModEntities;
import com.syyann.phantomcreeper.ModGameRules;

/**
 * 幻翼苦力怕：一只幻翼下面吊着一只苦力怕。
 * <p>
 * 没有目标时在空中盘旋；锁定玩家后直接俯冲，进入 {@link #TRIGGER_RADIUS} 格内点燃引信，
 * 引信燃尽时爆炸。继承 {@link Phantom} 是为了复用幻翼的模型、动画和白天自燃，
 * AI 和飞行控制则全部替换掉。
 */
public class PhantomCreeperEntity extends Phantom {
    /** 距离玩家多少格以内点燃引信 */
    public static final double TRIGGER_RADIUS = 6.0;
    /** 引信时长（tick，20 tick = 1 秒）。点燃后仍会继续俯冲，所以爆炸时基本已贴近玩家 */
    public static final int FUSE_TIME = 10;
    /** 爆炸威力，与普通苦力怕相同 */
    public static final float EXPLOSION_POWER = 3.0f;
    /** 锁定玩家的最大距离 */
    public static final double LOCK_ON_RANGE = 64.0;
    /** 连续看不到目标超过这么多 tick 就放弃锁定（例如玩家躲进屋里） */
    private static final int GIVE_UP_UNSEEN_TICKS = 100;
    /** 期望速度（格/tick），实际速度受空气阻力影响约为其 0.7 倍 */
    private static final double CIRCLE_SPEED = 0.45;
    private static final double DIVE_SPEED = 0.85;

    private static final EntityDataAccessor<Boolean> IGNITED =
            SynchedEntityData.defineId(PhantomCreeperEntity.class, EntityDataSerializers.BOOLEAN);

    /** 当前飞行目标点：由 AI 目标写入，飞行控制读取 */
    private Vec3 flightTarget = Vec3.ZERO;
    /** 无目标时盘旋的中心 */
    @Nullable
    private BlockPos anchor;
    private boolean diving;
    /** 同一批自然生成的幻翼苦力怕共享此 ID；刷怪蛋、指令召唤的没有编组（除非 NBT 指定） */
    @Nullable
    private UUID spawnGroup;
    private boolean exploding;
    private int lastFuseTime;
    private int currentFuseTime;

    public PhantomCreeperEntity(EntityType<? extends PhantomCreeperEntity> entityType, Level world) {
        super(entityType, world);
        this.moveControl = new FlightControl(this);
    }

    public static AttributeSupplier.Builder createPhantomCreeperAttributes() {
        return Monster.createMonsterAttributes();
    }

    @Override
    protected void registerGoals() {
        // 不调用 super：原版幻翼“盘旋 - 俯冲 - 拉起”的 AI 全部替换
        this.goalSelector.addGoal(1, new DiveAtTargetGoal());
        this.goalSelector.addGoal(2, new CircleGoal());
        this.targetSelector.addGoal(1, new LockOnPlayerGoal());
    }

    //? if >=1.20.5 {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IGNITED, false);
    }
    *///?} else {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IGNITED, false);
    }
    //?}

    @Override
    public void tick() {
        if (this.isAlive()) {
            this.lastFuseTime = this.currentFuseTime;
            if (this.isIgnited()) {
                if (this.currentFuseTime == 0) {
                    this.playSound(SoundEvents.CREEPER_PRIMED, 1.0f, 0.5f);
                    this.gameEvent(GameEvent.PRIME_FUSE);
                }
                if (++this.currentFuseTime >= FUSE_TIME) {
                    this.currentFuseTime = FUSE_TIME;
                    this.explode();
                }
            }
        }
        super.tick();
    }

    private void explode() {
        if (this.level().isClientSide() || this.exploding) {
            return;
        }
        this.exploding = true;
        this.dead = true;
        // ExplosionSourceType.MOB 会遵守 mobGriefing 游戏规则
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
        this.discard();
        this.detonateGroup();
    }

    /** 同一批生成的其他成员不论在哪、引信是否点燃，全部立刻引爆 */
    private void detonateGroup() {
        if (this.spawnGroup == null || !(this.level() instanceof ServerLevel serverWorld)) {
            return;
        }
        UUID group = this.spawnGroup;
        for (PhantomCreeperEntity member : serverWorld.getEntities(ModEntities.PHANTOM_CREEPER,
                entity -> entity.isAlive() && group.equals(entity.spawnGroup))) {
            member.explode();
        }
    }

    public void setSpawnGroup(@Nullable UUID spawnGroup) {
        this.spawnGroup = spawnGroup;
    }

    //? if >=1.21.2 {
    /*@Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource) {
        return this.isExplosionFromOwnGroup(damageSource) || super.isInvulnerableTo(level, damageSource);
    }
    *///?} else {
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return this.isExplosionFromOwnGroup(damageSource) || super.isInvulnerableTo(damageSource);
    }
    //?}

    /** 同组成员的爆炸炸不死自己，这样才能被连锁引爆，而不是先被炸死 */
    private boolean isExplosionFromOwnGroup(DamageSource damageSource) {
        return this.spawnGroup != null
                && damageSource.is(DamageTypeTags.IS_EXPLOSION)
                && damageSource.getDirectEntity() instanceof PhantomCreeperEntity other
                && this.spawnGroup.equals(other.spawnGroup);
    }

    public void ignite() {
        this.entityData.set(IGNITED, true);
    }

    public boolean isIgnited() {
        return this.entityData.get(IGNITED);
    }

    /** 客户端渲染用：引信进度（与原版苦力怕的膨胀/闪白算法一致） */
    public float getClientFuseTime(float tickDelta) {
        return Mth.lerp(tickDelta, (float) this.lastFuseTime, (float) this.currentFuseTime) / (float) (FUSE_TIME - 2);
    }

    // 开启白天刷新时不怕阳光，否则白天刷出来的会一直烧。
    // 1.21.11 起原版的晒伤判断改为私有、由实体标签控制，所以自己按原版算法实现一遍
    //? if >=1.21.11 {
    /*@Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel level
                && !ModGameRules.get(level, ModGameRules.DAYTIME_SPAWNING)
                && this.isInBurningSunlight()) {
            this.igniteForSeconds(8.0f);
        }
    }

    private boolean isInBurningSunlight() {
        if (!this.level().environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, this.position())) {
            return false;
        }
        float brightness = this.getLightLevelDependentMagicValue();
        BlockPos eyePos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
        boolean wet = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
        return brightness > 0.5f
                && this.random.nextFloat() * 30.0f < (brightness - 0.4f) * 2.0f
                && !wet
                && this.level().canSeeSky(eyePos);
    }
    *///?} else {
    @Override
    protected boolean isSunBurnTick() {
        return !ModGameRules.get(this.level(), ModGameRules.DAYTIME_SPAWNING) && super.isSunBurnTick();
    }
    //?}

    //? if <1.20.5 {
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        // 碰撞箱包含下方的苦力怕，眼睛在上方的幻翼身上（1.20.5 起改在 ModEntities 里设置）
        return dimensions.height * 0.85f;
    }
    //?}

    // 存档字段在各版本一致：AnchorX/Y/Z、ignited、Fuse、SpawnGroup（UUID）
    //? if >=1.21.6 {
    /*@Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.anchor != null) {
            output.putInt("AnchorX", this.anchor.getX());
            output.putInt("AnchorY", this.anchor.getY());
            output.putInt("AnchorZ", this.anchor.getZ());
        }
        output.putBoolean("ignited", this.isIgnited());
        output.putShort("Fuse", (short) this.currentFuseTime);
        output.storeNullable("SpawnGroup", UUIDUtil.CODEC, this.spawnGroup);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getInt("AnchorX").ifPresent(x -> this.anchor = new BlockPos(x, input.getIntOr("AnchorY", 0), input.getIntOr("AnchorZ", 0)));
        if (input.getBooleanOr("ignited", false)) {
            this.ignite();
        }
        this.currentFuseTime = input.getShortOr("Fuse", (short) 0);
        this.spawnGroup = input.read("SpawnGroup", UUIDUtil.CODEC).orElse(null);
        this.lastFuseTime = this.currentFuseTime;
    }
    *///?} else {
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.anchor != null) {
            nbt.putInt("AnchorX", this.anchor.getX());
            nbt.putInt("AnchorY", this.anchor.getY());
            nbt.putInt("AnchorZ", this.anchor.getZ());
        }
        nbt.putBoolean("ignited", this.isIgnited());
        nbt.putShort("Fuse", (short) this.currentFuseTime);
        if (this.spawnGroup != null) {
            nbt.putUUID("SpawnGroup", this.spawnGroup);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("AnchorX")) {
            this.anchor = new BlockPos(nbt.getInt("AnchorX"), nbt.getInt("AnchorY"), nbt.getInt("AnchorZ"));
        }
        if (nbt.getBoolean("ignited")) {
            this.ignite();
        }
        this.currentFuseTime = nbt.getShort("Fuse");
        if (nbt.hasUUID("SpawnGroup")) {
            this.spawnGroup = nbt.getUUID("SpawnGroup");
        }
        this.lastFuseTime = this.currentFuseTime;
    }
    //?}

    /** 自动锁定最近的、看得见的玩家；目标失效或长时间看不见时解除锁定 */
    class LockOnPlayerGoal extends Goal {
        private final TargetingConditions lockOnPredicate = TargetingConditions.forCombat().range(LOCK_ON_RANGE);
        private final TargetingConditions keepPredicate = TargetingConditions.forCombat().range(LOCK_ON_RANGE * 1.5).ignoreLineOfSight();
        private int searchDelay;
        private int unseenTicks;

        LockOnPlayerGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.searchDelay > 0) {
                this.searchDelay--;
                return false;
            }
            this.searchDelay = reducedTickDelay(20);
            //? if >=1.21.2 {
            /*Player player = ((ServerLevel) level()).getNearestPlayer(this.lockOnPredicate, PhantomCreeperEntity.this, getX(), getEyeY(), getZ());
            *///?} else {
            Player player = level().getNearestPlayer(this.lockOnPredicate, PhantomCreeperEntity.this, getX(), getEyeY(), getZ());
            //?}
            if (player == null) {
                return false;
            }
            setTarget(player);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            if (target == null || this.unseenTicks >= GIVE_UP_UNSEEN_TICKS) {
                return false;
            }
            //? if >=1.21.2 {
            /*return this.keepPredicate.test((ServerLevel) level(), PhantomCreeperEntity.this, target);
            *///?} else {
            return this.keepPredicate.test(PhantomCreeperEntity.this, target);
            //?}
        }

        @Override
        public void start() {
            this.unseenTicks = 0;
        }

        @Override
        public void stop() {
            setTarget(null);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && getSensing().hasLineOfSight(target)) {
                this.unseenTicks = 0;
            } else {
                this.unseenTicks++;
            }
        }
    }

    /** 锁定后直接朝玩家俯冲；进入触发半径且能看到玩家时点燃引信 */
    class DiveAtTargetGoal extends Goal {
        DiveAtTargetGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            diving = true;
            // 原版幻翼俯冲的尖啸声，给玩家一个预警
            playSound(SoundEvents.PHANTOM_SWOOP, 10.0f, 0.95f + getRandom().nextFloat() * 0.1f);
        }

        @Override
        public void stop() {
            diving = false;
            // 目标丢失后，在当前位置上空重新盘旋
            anchor = blockPosition().above(10);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            // 让实体底部（苦力怕）对准玩家身体，幻翼正好在玩家头顶
            flightTarget = target.position().add(0.0, target.getBbHeight() * 0.3, 0.0);
            if (!isIgnited()
                    && distanceToSqr(target) <= TRIGGER_RADIUS * TRIGGER_RADIUS
                    && hasLineOfSight(target)) {
                ignite();
            }
        }
    }

    /** 没有目标时绕着 anchor 盘旋（参考原版幻翼的盘旋逻辑） */
    class CircleGoal extends Goal {
        private float angle;
        private float radius;
        private float yOffset;
        private float direction;

        CircleGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null;
        }

        @Override
        public void start() {
            if (anchor == null) {
                anchor = blockPosition().above(5);
            }
            RandomSource random = getRandom();
            this.radius = 5.0f + random.nextFloat() * 10.0f;
            this.yOffset = -4.0f + random.nextFloat() * 9.0f;
            this.direction = random.nextBoolean() ? 1.0f : -1.0f;
            this.angle = random.nextFloat() * (float) (Math.PI * 2);
            this.nextPoint();
        }

        @Override
        public void tick() {
            RandomSource random = getRandom();
            if (random.nextInt(this.adjustedTickDelay(350)) == 0) {
                this.yOffset = -4.0f + random.nextFloat() * 9.0f;
            }
            if (random.nextInt(this.adjustedTickDelay(250)) == 0) {
                this.radius += 1.0f;
                if (this.radius > 15.0f) {
                    this.radius = 5.0f;
                    this.direction = -this.direction;
                }
            }
            if (random.nextInt(this.adjustedTickDelay(450)) == 0) {
                this.angle = random.nextFloat() * (float) (Math.PI * 2);
                this.nextPoint();
            }
            if (flightTarget.distanceToSqr(getX(), getY(), getZ()) < 4.0) {
                this.nextPoint();
            }
            // 别撞地面，也别撞天花板（碰撞箱约 2 格高）
            if (flightTarget.y < getY() && !level().isEmptyBlock(blockPosition().below(1))) {
                this.yOffset = Math.max(1.0f, this.yOffset);
                this.nextPoint();
            }
            if (flightTarget.y > getY() && !level().isEmptyBlock(blockPosition().above(2))) {
                this.yOffset = Math.min(-1.0f, this.yOffset);
                this.nextPoint();
            }
        }

        private void nextPoint() {
            if (anchor == null) {
                anchor = blockPosition().above(5);
            }
            this.angle += this.direction * 15.0f * Mth.DEG_TO_RAD;
            flightTarget = Vec3.atLowerCornerOf(anchor).add(
                    this.radius * Mth.cos(this.angle),
                    -4.0f + this.yOffset,
                    this.radius * Mth.sin(this.angle));
        }
    }

    /** 直接朝 flightTarget 飞，俯冲时更快、转向更灵活 */
    static class FlightControl extends MoveControl {
        private final PhantomCreeperEntity owner;
        private double flightSpeed = CIRCLE_SPEED;

        FlightControl(PhantomCreeperEntity owner) {
            super(owner);
            this.owner = owner;
        }

        @Override
        public void tick() {
            PhantomCreeperEntity e = this.owner;
            boolean charging = e.diving || e.isIgnited();
            double wanted = charging ? DIVE_SPEED : CIRCLE_SPEED;
            // 加速慢一点，减速快一点
            this.flightSpeed += Mth.clamp(wanted - this.flightSpeed, -0.05, 0.02);

            Vec3 delta = e.flightTarget.subtract(e.position());
            double distance = delta.length();
            if (distance < 1.0E-4) {
                return;
            }
            Vec3 desired = delta.scale(Math.min(this.flightSpeed, distance) / distance);
            Vec3 velocity = e.getDeltaMovement();
            velocity = velocity.add(desired.subtract(velocity).scale(charging ? 0.3 : 0.2));
            if (e.horizontalCollision) {
                // 撞墙时往上爬
                velocity = velocity.add(0.0, 0.1, 0.0);
            }
            e.setDeltaMovement(velocity);

            // 模型朝向飞行方向（俯仰角算法与原版幻翼一致，最大约 45°）
            double speed = velocity.length();
            if (speed > 0.02) {
                float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0f;
                e.setYRot(yaw);
                e.yBodyRot = yaw;
                e.yHeadRot = yaw;
                e.setXRot((float) -(Mth.atan2(-velocity.y, speed) * Mth.RAD_TO_DEG));
            }
        }
    }
}
