package com.syyann.phantomcreeper.entity;

import java.util.EnumSet;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.syyann.phantomcreeper.ModEntities;
import com.syyann.phantomcreeper.ModGameRules;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * 幻翼苦力怕：一只幻翼下面吊着一只苦力怕。
 * <p>
 * 没有目标时在空中盘旋；锁定玩家后直接俯冲，进入 {@link #TRIGGER_RADIUS} 格内点燃引信，
 * 引信燃尽时爆炸。继承 {@link PhantomEntity} 是为了复用幻翼的模型、动画和白天自燃，
 * AI 和飞行控制则全部替换掉。
 */
public class PhantomCreeperEntity extends PhantomEntity {
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

    private static final TrackedData<Boolean> IGNITED =
            DataTracker.registerData(PhantomCreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** 当前飞行目标点：由 AI 目标写入，飞行控制读取 */
    private Vec3d flightTarget = Vec3d.ZERO;
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

    public PhantomCreeperEntity(EntityType<? extends PhantomCreeperEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightControl(this);
    }

    public static DefaultAttributeContainer.Builder createPhantomCreeperAttributes() {
        return HostileEntity.createHostileAttributes();
    }

    @Override
    protected void initGoals() {
        // 不调用 super：原版幻翼“盘旋 - 俯冲 - 拉起”的 AI 全部替换
        this.goalSelector.add(1, new DiveAtTargetGoal());
        this.goalSelector.add(2, new CircleGoal());
        this.targetSelector.add(1, new LockOnPlayerGoal());
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(IGNITED, false);
    }

    @Override
    public void tick() {
        if (this.isAlive()) {
            this.lastFuseTime = this.currentFuseTime;
            if (this.isIgnited()) {
                if (this.currentFuseTime == 0) {
                    this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);
                    this.emitGameEvent(GameEvent.PRIME_FUSE);
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
        if (this.getWorld().isClient || this.exploding) {
            return;
        }
        this.exploding = true;
        this.dead = true;
        // ExplosionSourceType.MOB 会遵守 mobGriefing 游戏规则
        this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, World.ExplosionSourceType.MOB);
        this.discard();
        this.detonateGroup();
    }

    /** 同一批生成的其他成员不论在哪、引信是否点燃，全部立刻引爆 */
    private void detonateGroup() {
        if (this.spawnGroup == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        UUID group = this.spawnGroup;
        for (PhantomCreeperEntity member : serverWorld.getEntitiesByType(ModEntities.PHANTOM_CREEPER,
                entity -> entity.isAlive() && group.equals(entity.spawnGroup))) {
            member.explode();
        }
    }

    public void setSpawnGroup(@Nullable UUID spawnGroup) {
        this.spawnGroup = spawnGroup;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        // 同组成员的爆炸炸不死自己，这样才能被连锁引爆，而不是先被炸死
        if (this.spawnGroup != null
                && damageSource.isIn(DamageTypeTags.IS_EXPLOSION)
                && damageSource.getSource() instanceof PhantomCreeperEntity other
                && this.spawnGroup.equals(other.spawnGroup)) {
            return true;
        }
        return super.isInvulnerableTo(damageSource);
    }

    public void ignite() {
        this.dataTracker.set(IGNITED, true);
    }

    public boolean isIgnited() {
        return this.dataTracker.get(IGNITED);
    }

    /** 客户端渲染用：引信进度（与原版苦力怕的膨胀/闪白算法一致） */
    public float getClientFuseTime(float tickDelta) {
        return MathHelper.lerp(tickDelta, (float) this.lastFuseTime, (float) this.currentFuseTime) / (float) (FUSE_TIME - 2);
    }

    @Override
    protected boolean isAffectedByDaylight() {
        // 开启白天刷新时不怕阳光，否则白天刷出来的会一直烧
        return !this.getWorld().getGameRules().getBoolean(ModGameRules.DO_PHANTOM_CREEPER_DAYTIME_SPAWNING)
                && super.isAffectedByDaylight();
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        // 碰撞箱包含下方的苦力怕，眼睛在上方的幻翼身上
        return dimensions.height * 0.85f;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.anchor != null) {
            nbt.putInt("AnchorX", this.anchor.getX());
            nbt.putInt("AnchorY", this.anchor.getY());
            nbt.putInt("AnchorZ", this.anchor.getZ());
        }
        nbt.putBoolean("ignited", this.isIgnited());
        nbt.putShort("Fuse", (short) this.currentFuseTime);
        if (this.spawnGroup != null) {
            nbt.putUuid("SpawnGroup", this.spawnGroup);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("AnchorX")) {
            this.anchor = new BlockPos(nbt.getInt("AnchorX"), nbt.getInt("AnchorY"), nbt.getInt("AnchorZ"));
        }
        if (nbt.getBoolean("ignited")) {
            this.ignite();
        }
        this.currentFuseTime = nbt.getShort("Fuse");
        if (nbt.containsUuid("SpawnGroup")) {
            this.spawnGroup = nbt.getUuid("SpawnGroup");
        }
        this.lastFuseTime = this.currentFuseTime;
    }

    /** 自动锁定最近的、看得见的玩家；目标失效或长时间看不见时解除锁定 */
    class LockOnPlayerGoal extends Goal {
        private final TargetPredicate lockOnPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(LOCK_ON_RANGE);
        private final TargetPredicate keepPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(LOCK_ON_RANGE * 1.5).ignoreVisibility();
        private int searchDelay;
        private int unseenTicks;

        LockOnPlayerGoal() {
            this.setControls(EnumSet.of(Goal.Control.TARGET));
        }

        @Override
        public boolean canStart() {
            if (this.searchDelay > 0) {
                this.searchDelay--;
                return false;
            }
            this.searchDelay = toGoalTicks(20);
            PlayerEntity player = getWorld().getClosestPlayer(this.lockOnPredicate, PhantomCreeperEntity.this, getX(), getEyeY(), getZ());
            if (player == null) {
                return false;
            }
            setTarget(player);
            return true;
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = getTarget();
            return target != null
                    && this.unseenTicks < GIVE_UP_UNSEEN_TICKS
                    && this.keepPredicate.test(PhantomCreeperEntity.this, target);
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
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && getVisibilityCache().canSee(target)) {
                this.unseenTicks = 0;
            } else {
                this.unseenTicks++;
            }
        }
    }

    /** 锁定后直接朝玩家俯冲；进入触发半径且能看到玩家时点燃引信 */
    class DiveAtTargetGoal extends Goal {
        DiveAtTargetGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public void start() {
            diving = true;
            // 原版幻翼俯冲的尖啸声，给玩家一个预警
            playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 10.0f, 0.95f + getRandom().nextFloat() * 0.1f);
        }

        @Override
        public void stop() {
            diving = false;
            // 目标丢失后，在当前位置上空重新盘旋
            anchor = getBlockPos().up(10);
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            // 让实体底部（苦力怕）对准玩家身体，幻翼正好在玩家头顶
            flightTarget = target.getPos().add(0.0, target.getHeight() * 0.3, 0.0);
            if (!isIgnited()
                    && squaredDistanceTo(target) <= TRIGGER_RADIUS * TRIGGER_RADIUS
                    && canSee(target)) {
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
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return getTarget() == null;
        }

        @Override
        public void start() {
            if (anchor == null) {
                anchor = getBlockPos().up(5);
            }
            Random random = getRandom();
            this.radius = 5.0f + random.nextFloat() * 10.0f;
            this.yOffset = -4.0f + random.nextFloat() * 9.0f;
            this.direction = random.nextBoolean() ? 1.0f : -1.0f;
            this.angle = random.nextFloat() * (float) (Math.PI * 2);
            this.nextPoint();
        }

        @Override
        public void tick() {
            Random random = getRandom();
            if (random.nextInt(this.getTickCount(350)) == 0) {
                this.yOffset = -4.0f + random.nextFloat() * 9.0f;
            }
            if (random.nextInt(this.getTickCount(250)) == 0) {
                this.radius += 1.0f;
                if (this.radius > 15.0f) {
                    this.radius = 5.0f;
                    this.direction = -this.direction;
                }
            }
            if (random.nextInt(this.getTickCount(450)) == 0) {
                this.angle = random.nextFloat() * (float) (Math.PI * 2);
                this.nextPoint();
            }
            if (flightTarget.squaredDistanceTo(getX(), getY(), getZ()) < 4.0) {
                this.nextPoint();
            }
            // 别撞地面，也别撞天花板（碰撞箱约 2 格高）
            if (flightTarget.y < getY() && !getWorld().isAir(getBlockPos().down(1))) {
                this.yOffset = Math.max(1.0f, this.yOffset);
                this.nextPoint();
            }
            if (flightTarget.y > getY() && !getWorld().isAir(getBlockPos().up(2))) {
                this.yOffset = Math.min(-1.0f, this.yOffset);
                this.nextPoint();
            }
        }

        private void nextPoint() {
            if (anchor == null) {
                anchor = getBlockPos().up(5);
            }
            this.angle += this.direction * 15.0f * MathHelper.RADIANS_PER_DEGREE;
            flightTarget = Vec3d.of(anchor).add(
                    this.radius * MathHelper.cos(this.angle),
                    -4.0f + this.yOffset,
                    this.radius * MathHelper.sin(this.angle));
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
            this.flightSpeed += MathHelper.clamp(wanted - this.flightSpeed, -0.05, 0.02);

            Vec3d delta = e.flightTarget.subtract(e.getPos());
            double distance = delta.length();
            if (distance < 1.0E-4) {
                return;
            }
            Vec3d desired = delta.multiply(Math.min(this.flightSpeed, distance) / distance);
            Vec3d velocity = e.getVelocity();
            velocity = velocity.add(desired.subtract(velocity).multiply(charging ? 0.3 : 0.2));
            if (e.horizontalCollision) {
                // 撞墙时往上爬
                velocity = velocity.add(0.0, 0.1, 0.0);
            }
            e.setVelocity(velocity);

            // 模型朝向飞行方向（俯仰角算法与原版幻翼一致，最大约 45°）
            double speed = velocity.length();
            if (speed > 0.02) {
                float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
                e.setYaw(yaw);
                e.bodyYaw = yaw;
                e.headYaw = yaw;
                e.setPitch((float) -(MathHelper.atan2(-velocity.y, speed) * MathHelper.DEGREES_PER_RADIAN));
            }
        }
    }
}
