package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.entities.ai.DirectPathNavigator;
import com.fcs.locomotionmobs.entities.ai.GroundPathNavigatorWide;
import com.fcs.locomotionmobs.init.EntityInit;
import com.fcs.locomotionmobs.init.ItemInit;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class QueenBuzzlet extends Monster implements FlyingAnimal, RangedAttackMob {
    private static final EntityDataAccessor<Integer> ATTACK_TICK = SynchedEntityData.defineId(QueenBuzzlet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(QueenBuzzlet.class, EntityDataSerializers.BOOLEAN);
    protected static final float MOVEMENT_SPEED = 0.1F;
    protected static final float FLYING_SPEED = 0.1F;
    protected static final float MAX_HEALTH = 300;
    protected static final float ATTACK_DAMAGE = 3;
    protected static final float ARMOR = 1;
    protected static final float FOLLOW_RANGE = 25;
    private static final float ATTACK_SPEED = 1;

    private static final Component QUEEN_BUZZLET_EVENT_TITLE = new TextComponent("Queen Buzzlet");

    private final ServerBossEvent bossEvent = new ServerBossEvent(QUEEN_BUZZLET_EVENT_TITLE,
            ServerBossEvent.BossBarColor.PURPLE,
            ServerBossEvent.BossBarOverlay.NOTCHED_6);
    private QueenBuzzletPhase phase;
    private boolean isLandNavigator;
    private int timeFlying;
    private double orbitDist = 5D;
    private boolean orbitClockwise = false;
    private BlockPos orbitPos = null;
    public float prevAttackProgress;
    public float attackProgress;
    public float prevFlyProgress;
    public float flyProgress;
    public float prevSwoopProgress;
    public float swoopProgress;
    private float prevBirdPitch;
    private float birdPitch;
    private QueenGoToHiveGoal goToBaseGoal;

    @Override
    public void performRangedAttack(LivingEntity p_33317_, float p_33318_) {
        ThrownPotion potion = new ThrownPotion(this.level, this);
        Vec3 vec3 = p_33317_.getDeltaMovement();
        double d0 = p_33317_.getX() + vec3.x - this.getX();
        double d1 = p_33317_.getEyeY() - (double)1.1F - this.getY();
        double d2 = p_33317_.getZ() + vec3.z - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        potion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_POISON));
        potion.setXRot(potion.getXRot() + 20.0F);
        potion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
        this.level.addFreshEntity(potion);
    }

    private enum QueenBuzzletPhase{
        FULL, HALF, QUARTER
    }

    public QueenBuzzlet(PlayMessages.SpawnEntity packet, Level world){
        this(EntityInit.QUEEN_BUZZLET.get(), world);
    }

    public QueenBuzzlet(EntityType<QueenBuzzlet> entityType, Level level) {
        super(entityType, level);
        xpReward = 100;
        setCustomName(new TextComponent("Queen Buzzlet"));
        setCustomNameVisible(true);
        switchNavigator(true);
    }

    //This method is copied from https://github.com/AlexModGuy/AlexsMobs/blob/a6eded8bbe84b1bb93267ec5bf45356e4c179f99/src/main/java/com/github/alexthe666/alexsmobs/entity/EntityBaldEagle.java#L203
    private void switchNavigator(boolean onLand){
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new QueenBuzzlet.MoveHelper(this);
            this.navigation = new DirectPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, false);
        this.entityData.define(ATTACK_TICK, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            protected double getAttackReachSqr(@NotNull LivingEntity entity) {
                return (5.0 + entity.getBbWidth() * entity.getBbWidth());
            }
        });

        this.targetSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 20, 40, 15));
        this.targetSelector.addGoal(1, new StingerAttackGoal<QueenBuzzlet>(this, 1.0D, 10, 25));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RandomFlyGoal());
        this.goalSelector.addGoal(0, new QueenBuzzlet.QueenLocateHiveGoal());
        this.goToBaseGoal = new QueenBuzzlet.QueenGoToHiveGoal();
        this.goalSelector.addGoal(0, this.goToBaseGoal);
        this.goalSelector.addGoal(0, new RandomFlyGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 2) {
            @Override
            public boolean canUse() {
                return ((QueenBuzzlet)this.mob).phase == QueenBuzzletPhase.QUARTER && super.canUse();
            }

            @Override
            public void start() {
                bossEvent.getPlayers().forEach(p -> p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is Panicking"), ChatType.CHAT, p.getUUID())));
                super.start();
            }
        });

        this.targetSelector.addGoal(1, new QueenBuzzletSweepAttackGoal());
    }

    protected void dropCustomDeathLoot(@NotNull DamageSource p_31464_, int p_31465_, boolean p_31466_) {
        super.dropCustomDeathLoot(p_31464_, p_31465_, p_31466_);
        ItemEntity itementity = this.spawnAtLocation(Items.ENCHANTED_GOLDEN_APPLE);
        if (itementity != null) {
            itementity.setExtendedLifetime();
        }
    }
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    //This is important for the game server to communicate with the client. Even though we are playing in single player,
    //this is necessary in order to spawn the Queen Buzzlet in.
    @Override
    public @NotNull Packet<?> getAddEntityPacket(){
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void checkDespawn() {
        if(this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()){
            discard();
        }
    }

    @Override
    public boolean causeFallDamage(float funny, float numbers, @NotNull DamageSource meansNothing){
        return false;
    }

    @Override
    protected void checkFallDamage(double these, boolean numbers, @NotNull BlockState mean, @NotNull BlockPos nothing) {
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.bee.loop"));
    }

    @Override
    public SoundEvent getHurtSound(@NotNull DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    public @NotNull EntityType<? extends QueenBuzzlet> getType() {
        return (EntityType<? extends QueenBuzzlet>)super.getType();
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }
    //This is a method that is triggered when the player first sees the entity
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);

    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player){
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void customServerAiStep(){
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void tick(){
        super.tick();

        this.prevFlyProgress = flyProgress;
        this.prevAttackProgress = attackProgress;
        this.prevSwoopProgress = swoopProgress;
        this.prevBirdPitch = birdPitch;
        float yMot = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
        this.birdPitch = yMot;

        if(isFlying() && flyProgress < 5F){
            flyProgress++;
        }
        if(!isFlying() && flyProgress > 0F){
            flyProgress--;
        }
        if(yMot < 0.1F){
            if(swoopProgress > 0){
                swoopProgress--;
            }
        } else{
            if(swoopProgress < yMot * 0.2F){
                swoopProgress = Math.min(yMot * 0.2F, swoopProgress + 1);
            }
        }
        if(this.level.getNearestPlayer(this, 50) == null){
            if(getHealth() < getMaxHealth())
                this.heal(0.1f);
        }
        if(((this.getHealth() <= getMaxHealth() / 2) && (this.getHealth() > getMaxHealth() / 4)) && (this.phase != QueenBuzzletPhase.HALF)){
            this.phase = QueenBuzzletPhase.HALF;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED * 4);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(2);
            this.getAttribute(Attributes.ARMOR).setBaseValue(5);
            this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(FLYING_SPEED * 4);

            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is at phase Half"), ChatType.CHAT, p.getUUID()));});
        } else if ((this.getHealth() <= getMaxHealth() / 4) && (this.phase != QueenBuzzletPhase.QUARTER)){
            this.phase = QueenBuzzletPhase.QUARTER;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED * 8);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4);
            this.getAttribute(Attributes.ARMOR).setBaseValue(10);
            this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(FLYING_SPEED * 8);
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet at phase Quarter"), ChatType.CHAT, p.getUUID()));});
        } else if(this.getHealth() > this.getMaxHealth() / 2 && this.phase != QueenBuzzletPhase.FULL){
            this.phase = QueenBuzzletPhase.FULL;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1);
            this.getAttribute(Attributes.ARMOR).setBaseValue(ARMOR);
            this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(FLYING_SPEED);
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is phase Full"), ChatType.CHAT, p.getUUID()));});
        }
        //Some functionality below is copied from https://github.com/AlexModGuy/AlexsMobs/blob/a6eded8bbe84b1bb93267ec5bf45356e4c179f99/src/main/java/com/github/alexthe666/alexsmobs/entity/EntityBaldEagle.java#L203
        if(!level.isClientSide){
            if(isFlying() && this.isLandNavigator){
                switchNavigator(false);
            }
            if(!isFlying() && !this.isLandNavigator){
                switchNavigator(true);
            }
            if(isFlying()){
                timeFlying++;
                this.setNoGravity(true);
                if(this.getTarget() != null && this.getTarget().getY() < this.getX()){
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.9, 1.0));
                }
            }else {
                timeFlying = 0;
                this.setNoGravity(false);
            }
            if(this.getTarget() != null && this.isInWaterOrBubble()){
                timeFlying = 0;
                this.setFlying(true);
            }
            if(isFlying() && this.onGround && !this.isInWaterOrBubble() && this.timeFlying > 30){
                this.setFlying(false);
            }
        }
        if(this.entityData.get(ATTACK_TICK) > 0){
            if(this.entityData.get(ATTACK_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < this.getTarget().getBbWidth() + 2D){
                this.getTarget().hurt(DamageSource.mobAttack(this), 2);
            }
            this.entityData.set(ATTACK_TICK, this.entityData.get(ATTACK_TICK) - 1);
            if(attackProgress < 5F){
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F){
                attackProgress--;
            }
        }
    }

    //The next 7 methods were copied from https://github.com/AlexModGuy/AlexsMobs/blob/a6eded8bbe84b1bb93267ec5bf45356e4c179f99/src/main/java/com/github/alexthe666/alexsmobs/entity/EntityBaldEagle.java#L621
    //these methods were copied because I did not understand the funny math stuff

    public Vec3 getBlockInViewAway(Vec3 fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, 0, fleePos.z() + extraZ);
        BlockPos ground = getCrowGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 7 + this.getRandom().nextInt(10);
        BlockPos newPos = ground.above(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(7) + 4);
        if (!this.isTargetBlocked(Vec3.atCenterOf(newPos)) && this.distanceToSqr(Vec3.atCenterOf(newPos)) > 1) {
            return Vec3.atCenterOf(newPos);
        }
        return null;
    }

    private BlockPos getCrowGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), this.getY(), in.getZ());
        while (position.getY() < 320 && !level.getFluidState(position).isEmpty()) {
            position = position.above();
        }
        while (position.getY() > -64 && !level.getBlockState(position).getMaterial().isSolidBlocking()) {
            position = position.below();
        }
        return position;
    }

    public Vec3 getBlockGrounding(Vec3 fleePos) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, getY(), fleePos.z() + extraZ);
        BlockPos ground = this.getCrowGround(radialPos);
        if (ground.getY() == -64) {
            return this.position();
        } else {
            ground = this.blockPosition();
            while (ground.getY() > -64 && !level.getBlockState(ground).getMaterial().isSolidBlocking()) {
                ground = ground.below();
            }
        }
        if (!this.isTargetBlocked(Vec3.atCenterOf(ground.above()))) {
            return Vec3.atCenterOf(ground);
        }
        return null;
    }

    public boolean isTargetBlocked(Vec3 target) {
        Vec3 Vector3d = new Vec3(this.getX(), this.getEyeY(), this.getZ());

        return this.level.clip(new ClipContext(Vector3d, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS;
    }

    private Vec3 getOrbitVec(Vec3 vector3d, float gatheringCircleDist) {
        float angle = (0.01745329251F * (float) this.orbitDist * (orbitClockwise ? -tickCount : tickCount));
        double extraX = gatheringCircleDist * Mth.sin((angle));
        double extraZ = gatheringCircleDist * Mth.cos(angle);
        if (this.orbitPos != null) {
            Vec3 pos = new Vec3(orbitPos.getX() + extraX, orbitPos.getY() + random.nextInt(2) - 2, orbitPos.getZ() + extraZ);
            if (this.level.isEmptyBlock(new BlockPos(pos))) {
                return pos;
            }
        }
        return null;
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.blockPosition();
        while (position.getY() > -64 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        return !level.getFluidState(position).isEmpty() || position.getY() <= -64;
    }

    //This is another important one. You can change the values in here to change the corresponding attribute,
    //but this is also something that is required to spawn Queen Buzzlet
    public static AttributeSupplier.Builder createAttributes(){
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED);//1f
        builder = builder.add(Attributes.MAX_HEALTH, MAX_HEALTH);//300
        builder = builder.add(Attributes.ARMOR, ARMOR);//0
        builder = builder.add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);//3
        builder = builder.add(Attributes.FLYING_SPEED, FLYING_SPEED);//0.4f
        builder = builder.add(Attributes.ATTACK_SPEED, ATTACK_SPEED);//2f
        builder = builder.add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);//25
        return builder;
    }

    @Override
    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
    }

    //This Method was taken from net.minecraft.world.entity.animal.Bee;
    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level p_27815_) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_27815_) {
            public boolean isStableDestination(BlockPos p_27947_) {
                return !this.level.getBlockState(p_27947_.below()).isAir();
            }

            public void tick() {
                super.tick();
            }
        };
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(false);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    //THIS CLASS IS COPIED FROM https://github.com/AlexModGuy/AlexsMobs/blob/a6eded8bbe84b1bb93267ec5bf45356e4c179f99/src/main/java/com/github/alexthe666/alexsmobs/entity/EntityBaldEagle.java#L899
    class MoveHelper extends MoveControl{
        private final QueenBuzzlet parentEntity;

        public MoveHelper(QueenBuzzlet queenBuzzlet) {
            super(queenBuzzlet);
            this.parentEntity = queenBuzzlet;
        }

        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                Vec3 vector3d = new Vec3(this.wantedX - parentEntity.getX(), this.wantedY - parentEntity.getY(), this.wantedZ - parentEntity.getZ());
                double d5 = vector3d.length();
                if (d5 < 0.3) {
                    this.operation = MoveControl.Operation.WAIT;
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().scale(0.5D));
                } else {
                    double d0 = this.wantedX - this.parentEntity.getX();
                    double d1 = this.wantedY - this.parentEntity.getY();
                    double d2 = this.wantedZ - this.parentEntity.getZ();
                    double d3 = Mth.sqrt((float) (d0 * d0 + d1 * d1 + d2 * d2));
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(vector3d.scale(this.speedModifier * 0.05D / d5)));
                    Vec3 vector3d1 = parentEntity.getDeltaMovement();
                    parentEntity.setYRot(-((float) Mth.atan2(vector3d1.x, vector3d1.z)) * (180F / (float) Math.PI));
                    parentEntity.yBodyRot = parentEntity.getYRot();

                }

            }
        }

        private boolean canReach(Vec3 p_220673_1_, int p_220673_2_) {
            AABB axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.move(p_220673_1_);
                if (!this.parentEntity.level.noCollision(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }


    public static class StingerAttackGoal<T extends net.minecraft.world.entity.Mob & RangedAttackMob> extends RangedBowAttackGoal<T>{
        //used to simulate charging a shot. this is just the timer
        private int attackTime = -1;
        //this defines what an arrow is
        private final StingerItem stinger;
        //private ArrowItem stinger = (ArrowItem)Items.ARROW;
        //setting up a cooldown timer so Queen Buzzlet doesn't just spam arrows
        private int cooldownTimer = 5;
        //yes game, Queen Buzzlet totally has "Arrows" in its inventory
        private final ItemStack stingers;
        private final QueenBuzzlet usingEntity;
        private final int attackInterval;

        @Override
        protected boolean isHoldingBow() {
            return true;
        }

        public StingerAttackGoal(QueenBuzzlet usingEntity, double speedMod, int attackInterval, float attackRadius) {
            super(usingEntity, speedMod, attackInterval, attackRadius);
            this.attackInterval = attackInterval;
            this.usingEntity = usingEntity;
            stinger = (StingerItem) ItemInit.QUEEN_BUZZLET_STINGER.get();
            stingers = new ItemStack(() -> stinger);
        }

        @Override
        public boolean canUse() {
            if(usingEntity.phase != QueenBuzzletPhase.QUARTER) {
                //check the cooldown timer to use
                if (cooldownTimer == 0) {
                    return super.canUse();
                } else {
                    cooldownTimer--;
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            LivingEntity target = usingEntity.getTarget();
            super.tick();
            if(attackTime > attackInterval){
                QueenBuzzletStinger fireableStinger = stinger.createStinger(usingEntity.getLevel(), stingers, usingEntity);

                //the fifth parameter adjusts power of the stinger
                //abstractStinger.shootFromRotation(usingEntity, usingEntity.getXRot(), usingEntity.getYRot(), 0.0F, 3, 1.0F);
                //the damage dealt by the stinger
                fireableStinger.setBaseDamage(2.0D);
//                //knockback of the stinger
                fireableStinger.setKnockback(2);
                if(target != null && target.isAlive()){
                    usingEntity.getLookControl().setLookAt(target);
                    if(usingEntity.getLookControl().isLookingAtTarget()) {
                        //These 4 variables get the distances to pass into the shoot method
                        //From here to abstractStinger.shoot(double, double, double, float, float) is from the Skeleton class
                        double targetX = target.getX() - usingEntity.getX();
                        double targetY = target.getY((1.0D / 3.0D)) - fireableStinger.getY();
                        double targetZ = target.getZ() - usingEntity.getZ();
                        double targetLine = Math.sqrt((targetX * targetX) + (targetZ * targetZ));
                        //The 4th parameter seems to set the damage, and my best estimate of the 5th is some sort of scaling based on difficulty (maybe accuracy)
                        fireableStinger.shoot(targetX, targetY + targetLine * 0.2D, targetZ, 2.0F, (float) (14 - usingEntity.level.getDifficulty().getId() * 4));

                        //successful target , make new stinger
                        usingEntity.getLevel().addFreshEntity(fireableStinger);
                        //stinger hurts after fire. I figured STARVE is best because it is hurting itself, though if it dies from
                        //firing an arrow it will say "Queen Buzzlet Starved to death"
                        usingEntity.hurt(DamageSource.STARVE, 1);
                        //reset time to attack
                        attackTime = 0;
                        //reset cooldown timer
                        cooldownTimer = attackInterval;
                    }
                }
            }
            attackTime++;
        }
    }

    //this class has code that is heavily influenced by https://github.com/AlexModGuy/AlexsMobs/blob/a6eded8bbe84b1bb93267ec5bf45356e4c179f99/src/main/java/com/github/alexthe666/alexsmobs/entity/EntityBaldEagle.java#L935
    public class RandomFlyGoal extends Goal{
        protected final QueenBuzzlet queen;
        protected double x;
        protected double y;
        protected double z;
        private boolean targetInAir;
        private int cooldown = 0;
        private int maxFlyTime = 500;
        private int flyTime = 0;

        RandomFlyGoal(){
            super();
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            this.queen = QueenBuzzlet.this;
        }

        @Override
        public boolean canUse() {
            if(cooldown < 0){
                cooldown++;
            }
            if(this.queen.getRandom().nextInt(15) != 0 && !queen.isFlying()){
                return false;
            }
            if(queen.isInWaterOrBubble()){
                this.targetInAir = true;
            } else if(queen.isOnGround()){
                this.targetInAir = random.nextBoolean();
            } else {
                if(cooldown == 0 && random.nextInt(6) == 0){
                    cooldown = 400;
                    queen.orbitPos = queen.blockPosition();
                    queen.orbitDist = 4 + random.nextInt(5);
                    queen.orbitClockwise = random.nextBoolean();
                    flyTime = 0;
                    maxFlyTime = (int) (random.nextFloat() * 400.0F + 100.0F);
                }
                this.targetInAir = true;
            }

            Vec3 currentPos = this.findPos();
            if(currentPos == null){
                return false;
            } else{
                this.x = currentPos.x;
                this.y = currentPos.y;
                this.z = currentPos.z;
                return true;
            }
        }

        public void tick(){
            if(cooldown > 0){
                cooldown--;
            }
            if(cooldown < 0){
                cooldown++;
            }
            if(cooldown > 0 && queen.orbitPos != null){
                if(flyTime < maxFlyTime && !queen.isInWaterOrBubble()){
                    flyTime++;
                } else {
                    flyTime = 0;
                    queen.orbitPos = null;
                    cooldown = -400 - random.nextInt(400);
                }
            }
            if(queen.horizontalCollision && queen.isFlying()){
                stop();
            }
            if(targetInAir){
                queen.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                if(queen.isFlying() && !queen.onGround) {
                    if(queen.isInWaterOrBubble()) {
                        queen.setDeltaMovement(queen.getDeltaMovement().multiply(1.2F, 0.6F, 1.2F));
                    } else {
                        queen.getNavigation().moveTo(x, y, z, 1F);
                    }
                }
                if(!targetInAir && queen.isFlying() && queen.onGround){
                    queen.setFlying(false);
                    flyTime = 0;
                    queen.orbitPos = null;
                    cooldown = -400 - random.nextInt(400);
                }
                if(queen.isFlying() && (!level.isEmptyBlock(queen.getBlockPosBelowThatAffectsMyMovement()) || !queen.isFlying()) && !queen.isInWaterOrBubble() && flyTime > 30){
                    queen.setFlying(false);
                    flyTime = 0;
                    queen.orbitPos = null;
                    cooldown = -400 - random.nextInt(400);
                }
            }
        }

        @Override
        public boolean canContinueToUse(){
            if(targetInAir){
                return queen.isFlying() && queen.distanceToSqr(x, y, z) > 2F;
            }else {
                return (!queen.getNavigation().isDone());
            }
        }
        public void stop(){
            queen.getNavigation().stop();
            super.stop();
        }

        @Override
        public void start(){
            if(targetInAir){
                queen.setFlying(true);
                queen.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else{
                queen.getNavigation().moveTo(x, y, z, 1F);
            }
        }

        private Vec3 findPos() {
            Vec3 position = queen.position();
            if(cooldown > 0 && queen.orbitPos != null){
                return queen.getOrbitVec(position, 4 + random.nextInt(2));
            }
            if(queen.isOverWaterOrVoid()){
                targetInAir = true;
            }
            if(targetInAir){
                if(queen.timeFlying < 500 || queen.isOverWaterOrVoid()){
                    return queen.getBlockInViewAway(position, 0);
                } else{
                    return queen.getBlockGrounding(position);
                }
            } else {
                return LandRandomPos.getPos(queen, 1, 7);
            }
//            Vec3 position = queen.getViewVector(0.0F + new Random().nextFloat(-1.0F, 1.0F));
//            Vec3 randomPosition = HoverRandomPos.getPos(queen, 8, 7, position.x, position.z, ((float) Math.PI / 3F), 3, 1);
//            return randomPosition != null ? randomPosition : AirAndWaterRandomPos.getPos(queen, 8, 4, -2, position.x, position.z, (double) ((float) Math.PI / 2F));
        }
    }

    class QueenBuzzletSweepAttackGoal extends Goal {

        Vec3 moveTargetPoint = Vec3.ZERO;

        Vec3 setRealZero = new Vec3(0.0D, 60.0D, 0.0D);

        public void setMoveTargetPoint(Vec3 moveTargetPoint) {
            this.moveTargetPoint = moveTargetPoint.subtract(moveTargetPoint);
        }

        int cooldown;

        public boolean canUse() {
            if (cooldown <= 0) {
                setMoveTargetPoint(setRealZero);
                cooldown--;
                return true;
            } else {

                return false;

            }
        }

        public boolean canContinueToUse() {
            LivingEntity livingentity = QueenBuzzlet.this.getTarget();
            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else {
                if (livingentity instanceof Player player) {
                    if (livingentity.isSpectator() || player.isCreative()) {
                        return false;
                    }
                }

                if (!this.canUse()) {
                    if (cooldown <= 0) {
                        cooldown--;
                        return true;
                    } else {
                        return false;
                    }
                }

            }

            return false;

        }

        public void tick() {
            LivingEntity livingentity = QueenBuzzlet.this.getTarget();
            if (livingentity != null) {
                moveTargetPoint = new Vec3(livingentity.getX(), livingentity.getY(0.5D), livingentity.getZ());
                if (QueenBuzzlet.this.getBoundingBox().inflate(0.2F).intersects(livingentity.getBoundingBox())) {
                    QueenBuzzlet.this.doHurtTarget(livingentity);
                    if (!QueenBuzzlet.this.isSilent()) {
                        QueenBuzzlet.this.level.levelEvent(1, QueenBuzzlet.this.blockPosition(), 0);
                    }
                }

            }

        }

        @Override
        public void start() {
            super.start();
            cooldown = 10;
        }
    }

    // Go Home Goal--------------------------------------------------------------------------------------------

    int remainingCooldownBeforeLocatingHive;

    @Nullable
    BlockPos hivePos;

    @VisibleForDebug
    public boolean hasHive() {
        return this.hivePos != null;
    }

    // From the EntityInit Class (Finds Paths in Three Dimensional Space)
    void pathfindRandomlyTowards(BlockPos p_27881_) {
        Vec3 vec3 = Vec3.atBottomCenterOf(p_27881_);
        int i = 0;
        BlockPos blockpos = this.blockPosition();
        int j = (int) vec3.y - blockpos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int i1 = blockpos.distManhattan(p_27881_);
        if (i1 < 15) {
            k = i1 / 2;
            l = i1 / 2;
        }

        Vec3 vec31 = AirRandomPos.getPosTowards(this, k, l, i, vec3, (double) ((float) Math.PI / 10F));
        if (vec31 != null) {
            this.navigation.setMaxVisitedNodesMultiplier(0.5F);
            this.navigation.moveTo(vec31.x, vec31.y, vec31.z, 1.0D);
        }
    }
//test
    boolean closerThan(BlockPos p_27817_, int p_27818_) {
        return p_27817_.closerThan(this.blockPosition(), (double) p_27818_);
    }

    boolean isTooFarAway(BlockPos p_27890_) {
        return !this.closerThan(p_27890_, 100);
    }


    abstract class QueenGoals extends Goal {
        public abstract boolean canQueenUse();

        public abstract boolean canQueenContinueToUse();

        //&& !isAngry()
        public boolean canUse() {
            return this.canQueenUse();
        }

        public boolean canContinueToUse() {
            return this.canQueenContinueToUse();
        }
    }

    // Based off the Bee class BeeLocateHiveGoal
    class QueenLocateHiveGoal extends QueenBuzzlet.QueenGoals {
        public boolean canQueenUse() {
            return QueenBuzzlet.this.remainingCooldownBeforeLocatingHive == 0 && !QueenBuzzlet.this.hasHive();
        }

        public boolean canQueenContinueToUse() {
            return false;
        }

        public void start() {
            QueenBuzzlet.this.remainingCooldownBeforeLocatingHive = 200;
            List<BlockPos> list = this.findNearbyHivesWithSpace();
            if (!list.isEmpty()) {
                for (BlockPos blockpos : list) {
                    if (!QueenBuzzlet.this.goToBaseGoal.isTargetBlacklisted(blockpos)) {
                        QueenBuzzlet.this.hivePos = blockpos;
                        return;
                    }
                }

                QueenBuzzlet.this.goToBaseGoal.clearBlacklist();
                QueenBuzzlet.this.hivePos = list.get(0);
            }
        }

        private List<BlockPos> findNearbyHivesWithSpace() {
            BlockPos blockpos = QueenBuzzlet.this.blockPosition();
            PoiManager poimanager = ((ServerLevel) QueenBuzzlet.this.level).getPoiManager();
            Stream<PoiRecord> stream = poimanager.getInRange((p_28045_) -> {
                return p_28045_ == PoiType.BEEHIVE || p_28045_ == PoiType.BEE_NEST;
            }, blockpos, 20, PoiManager.Occupancy.ANY);
            return stream.map(PoiRecord::getPos).sorted(Comparator.comparingDouble((p_148811_) -> {
                return p_148811_.distSqr(blockpos);
            })).collect(Collectors.toList());
        }
    }

    // Based off the Bee class BeeGoToHive Goal
    @VisibleForDebug
    public class QueenGoToHiveGoal extends QueenBuzzlet.QueenGoals {
        int travellingTicks = QueenBuzzlet.this.level.random.nextInt(10);
        final List<BlockPos> blacklistedTargets = Lists.newArrayList();
        @Nullable
        private Path lastPath;
        private int ticksStuck;

        QueenGoToHiveGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean canQueenUse() {
            return QueenBuzzlet.this.hivePos != null && !QueenBuzzlet.this.hasRestriction() && !this.hasReachedTarget(QueenBuzzlet.this.hivePos)
                    && QueenBuzzlet.this.level.getBlockState(QueenBuzzlet.this.hivePos).is(BlockTags.BEEHIVES);
        }

        public boolean canQueenContinueToUse() {
            return this.canQueenUse();
        }

        public void start() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            super.start();
        }

        public void stop() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            QueenBuzzlet.this.navigation.stop();
            QueenBuzzlet.this.navigation.resetMaxVisitedNodesMultiplier();
        }

        public void tick() {
            if (QueenBuzzlet.this.hivePos != null) {
                ++this.travellingTicks;
                if (this.travellingTicks > this.adjustedTickDelay(600)) {
                    this.dropAndBlacklistHive();
                } else if (!QueenBuzzlet.this.navigation.isInProgress()) {
                    if (!QueenBuzzlet.this.closerThan(QueenBuzzlet.this.hivePos, 16)) {
                        if (QueenBuzzlet.this.isTooFarAway(QueenBuzzlet.this.hivePos)) {
                            this.dropHive();
                        } else {
                            QueenBuzzlet.this.pathfindRandomlyTowards(QueenBuzzlet.this.hivePos);
                        }
                    } else {
                        boolean flag = this.pathfindDirectlyTowards(QueenBuzzlet.this.hivePos);
                        if (!flag) {
                            this.dropAndBlacklistHive();
                        } else if (this.lastPath != null && QueenBuzzlet.this.navigation.getPath().sameAs(this.lastPath)) {
                            ++this.ticksStuck;
                            if (this.ticksStuck > 60) {
                                this.dropHive();
                                this.ticksStuck = 0;
                            }
                        } else {
                            this.lastPath = QueenBuzzlet.this.navigation.getPath();
                        }

                    }
                }
            }
        }

        private boolean pathfindDirectlyTowards(BlockPos p_27991_) {
            QueenBuzzlet.this.navigation.setMaxVisitedNodesMultiplier(10.0F);
            QueenBuzzlet.this.navigation.moveTo(p_27991_.getX(), p_27991_.getY(), p_27991_.getZ(), 1.0D);
            return QueenBuzzlet.this.navigation.getPath() != null && QueenBuzzlet.this.navigation.getPath().canReach();
        }

        boolean isTargetBlacklisted(BlockPos p_27994_) {
            return this.blacklistedTargets.contains(p_27994_);
        }

        private void blacklistTarget(BlockPos p_27999_) {
            this.blacklistedTargets.add(p_27999_);

            while (this.blacklistedTargets.size() > 3) {
                this.blacklistedTargets.remove(0);
            }

        }

        void clearBlacklist() {
            this.blacklistedTargets.clear();
        }

        private void dropAndBlacklistHive() {
            if (QueenBuzzlet.this.hivePos != null) {
                this.blacklistTarget(QueenBuzzlet.this.hivePos);
            }

            this.dropHive();
        }

        private void dropHive() {
            QueenBuzzlet.this.hivePos = null;
            QueenBuzzlet.this.remainingCooldownBeforeLocatingHive = 200;
        }

        private boolean hasReachedTarget(BlockPos p_28002_) {
            if (QueenBuzzlet.this.closerThan(p_28002_, 2)) {
                return true;
            } else {
                Path path = QueenBuzzlet.this.navigation.getPath();
                return path != null && path.getTarget().equals(p_28002_) && path.canReach() && path.isDone();
            }
        }
    }
}



