package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.Random;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;


public class QueenBuzzlet extends Monster implements FlyingAnimal, NeutralMob {
    //I believe that this is what you use to tell the game that this Monster is flying
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(QueenBuzzlet.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(),
            ServerBossEvent.BossBarColor.WHITE,
            ServerBossEvent.BossBarOverlay.PROGRESS);

    public QueenBuzzlet(PlayMessages.SpawnEntity packet, Level world){
        this(EntityInit.QUEEN_BUZZLET.get(), world);
    }
    public QueenBuzzlet(EntityType<QueenBuzzlet> entityType, Level level) {
        super(entityType, level);
        xpReward = 100;
        setCustomName(new TextComponent("Queen Buzzlet"));
        setCustomNameVisible(true);
        setNoGravity(true);
        this.moveControl = new FlyingMoveControl(this, 20 , true);
        //set this to false to enable AI. I was using this to adjust the hit box and the model
        setNoAi(false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return (double) (4.0 + entity.getBbWidth() * entity.getBbWidth());
            }
        });
        this.goalSelector.addGoal(1, new RandomFlyGoal());
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(2, new StingerAttackGoal<>(this, 1.0D, 10, 15));
    }

    protected void dropCustomDeathLoot(DamageSource p_31464_, int p_31465_, boolean p_31466_) {
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
    public Packet<?> getAddEntityPacket(){
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void checkDespawn() {
        if(this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()){
            discard();
        }
    }

//
    //For ambient bee noises
//    @Override
//    public SoundEvent getAmbientSound() {
//        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.bee.loop"));
//    }
//
    //for getting hurt noises
//    @Override
//    public SoundEvent getHurtSound(DamageSource ds) {
//        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
//    }
//
    //for death noises
//    @Override
//    public SoundEvent getDeathSound() {
//        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
//    }
//
    //tells minecraft if this entity can go through a portal *Basically*
//    @Override
//    public boolean canChangeDimensions() {
//        return false;
//    }
//
    @Override
    public MobType getMobType(){
        return MobType.UNDEFINED;
    }
    //This is a method that is triggered when the player first sees the entity
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player){
        super.stopSeenByPlayer(player);
        bossInfo.removePlayer(player);
    }

    @Override
    public void customServerAiStep(){
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void tick(){
        super.tick();
        if(this.level.getNearestPlayer(this, 50) == null){
            if(getHealth() < getMaxHealth())
                this.heal(0.1f);
        }
    }



    //If we wanted to spawn our mob in naturally in the world, we would use this. This may be useful if we end up making more mobs
//    public static void init() {
//        SpawnPlacements.register(EntityInit.QUEEN_BUZZLET.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
//                (entityType, world, reason, pos, random) -> (world.getDifficulty() != Difficulty.PEACEFUL
//                        && Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
//    }

    //This is another important one. You can change the values in here to change the corresponding attribute,
    //but this is also something that is required to spawn Queen Buzzlet
    public static AttributeSupplier.Builder createAttributes(){
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
        builder = builder.add(Attributes.MAX_HEALTH, 300);
        builder = builder.add(Attributes.ARMOR, 0);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
        builder = builder.add(Attributes.FLYING_SPEED, 1.2F);
        builder = builder.add(Attributes.FOLLOW_RANGE, 25);
        return builder;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return 0;
    }

    @Override
    public void setRemainingPersistentAngerTime(int p_21673_) {

    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return null;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID p_21672_) {

    }

    @Override
    public void startPersistentAngerTimer() {

    }

    public class StingerAttackGoal<T extends Mob & RangedAttackMob> extends RangedBowAttackGoal{
        //used to simulate charging a shot. this is just the timer
        private int attackTime = -1;
        //this defines what an arrow is
        private ArrowItem stinger = (ArrowItem) Items.ARROW;

        //setting up a cooldown timer so Queen Buzzlet doesn't just spam arrows
        private int cooldownTimer = 0;
        //yes game, Queen Buzzlet totally has "Arrows" in its inventory
        private ItemStack stingers = new ItemStack(() -> Items.ARROW);
        private QueenBuzzlet usingEntity;
        private final int attackInterval;
        public StingerAttackGoal(QueenBuzzlet usingEntity, double speedMod, int attackInterval, float attackRadius) {
            super(usingEntity, speedMod, attackInterval, attackRadius);
            this.attackInterval = attackInterval;
            this.usingEntity = usingEntity;
        }

        @Override
        public boolean canUse() {
            //check the cooldown timer to use
            if(cooldownTimer == 0) {
                return true;
            }else {
                cooldownTimer--;
                return false;
            }
        }

        @Override
        protected boolean isHoldingBow() {
            //yes game it's holding a "Bow"
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = usingEntity.getTarget();
            super.tick();
            if(attackTime > attackInterval){
                AbstractArrow abstractStinger = stinger.createArrow(usingEntity.getLevel(), stingers, usingEntity);

                //the fifth parameter adjusts power of the stinger
                //abstractStinger.shootFromRotation(usingEntity, usingEntity.getXRot(), usingEntity.getYRot(), 0.0F, 3, 1.0F);
                //the damage dealt by the stinger
                abstractStinger.setBaseDamage(2.0D);
                //knockback of the stinger
                abstractStinger.setKnockback(2);
                if(target != null && target.isAlive()){
                    usingEntity.getLookControl().setLookAt(target);
                    if(usingEntity.getLookControl().isLookingAtTarget()) {
                        //These 4 variables get the distances to pass into the shoot method
                        //From here to abstractStinger.shoot(double, double, double, float, float) is from the Skeleton class
                        double targetX = target.getX() - usingEntity.getX();
                        double targetY = target.getY((1.0D / 3.0D)) - abstractStinger.getY();
                        double targetZ = target.getZ() - usingEntity.getZ();
                        double targetLine = Math.sqrt((targetX * targetX) + (targetZ * targetZ));
                        //The 4th parameter seems to set the damage, and my best estimate of the 5th is some sort of scaling based on difficulty (maybe accuracy)
                        abstractStinger.shoot(targetX, targetY + targetLine * 0.2D, targetZ, 2.0F, (float) (14 - usingEntity.level.getDifficulty().getId() * 4));

                        //successful target , make new stinger
                        usingEntity.getLevel().addFreshEntity(abstractStinger);
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

    public class RandomFlyGoal extends Goal{
        private int cooldown = 0;

        RandomFlyGoal(){
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if(cooldown <= 0) {
                return QueenBuzzlet.this.navigation.isDone();
            }
            else{
                cooldown--;
                return false;
            }
        }

        @Override
        public boolean canContinueToUse(){
            return QueenBuzzlet.this.navigation.isInProgress();
        }

        @Override
        public void start(){
            Vec3 randomPosition = this.findPos();
            if(randomPosition != null) {
                QueenBuzzlet.this.navigation.moveTo(QueenBuzzlet.this.navigation.createPath(new BlockPos(randomPosition), 3), 3.0D);
                cooldown = 10;
            }
        }

        private Vec3 findPos(){
            Vec3 position = QueenBuzzlet.this.getViewVector(0.0F + new Random().nextFloat(-1.0F, 1.0F));
            Vec3 randomPosition = HoverRandomPos.getPos(QueenBuzzlet.this, 10, 5, position.x, position.z, ((float)Math.PI / 3F), 1, 1);
            return randomPosition != null ? randomPosition : AirAndWaterRandomPos.getPos(QueenBuzzlet.this, 8, 4, 2, position.x, position.z, (double)((float)Math.PI / 2F));
        }

    }
}
