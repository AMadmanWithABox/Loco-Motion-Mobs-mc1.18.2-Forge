package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.entities.util.QueenBuzzletEvent;
import com.fcs.locomotionmobs.init.EntityInit;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;


public class QueenBuzzlet extends Monster implements FlyingAnimal{
    private final EntityType<QueenBuzzlet> entityType;
    private boolean isOwnerOfEvent;
    protected static final float MOVEMENT_SPEED = 1F;
    protected static final float FLYING_SPEED = 2F;
    protected static final float MAX_HEALTH = 300;
    protected static final float ATTACK_DAMAGE = 3;
    protected static final float ARMOR = 1;
    protected static final float FOLLOW_RANGE = 25;
    protected final int SIZE;
    private QueenBuzzletEvent event;
    //I believe that this is what you use to tell the game that this Monster is flying
    private Vec3 hoverPos;
    private QueenBuzzletPhase phase;
    private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(),
            ServerBossEvent.BossBarColor.WHITE,
            ServerBossEvent.BossBarOverlay.PROGRESS);

    private enum QueenBuzzletPhase{
        FULL, HALF, QUARTER
    }

    public QueenBuzzlet(PlayMessages.SpawnEntity packet, Level world){
        this(EntityInit.QUEEN_BUZZLET.get(), world);
    }
    public QueenBuzzlet(EntityType<QueenBuzzlet> entityType, Level level) {
        super(entityType, level);
        this.isOwnerOfEvent = true;
        this.SIZE = 3;
        this.entityType = entityType;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 100;
        setCustomName(new TextComponent("Queen Buzzlet"));
        setCustomNameVisible(true);
        //setNoGravity(true);
        this.navigation.setCanFloat(true);
        this.event = QueenBuzzletEvent.startNewEvent(this);
        //set this to false to enable AI. I was using this to adjust the hit box and the model
        //setNoAi(false);
    }
    //this is the constructor for the entity when it is spawned via the split method
    public QueenBuzzlet(QueenBuzzlet parent, int size){
        super(parent.entityType, parent.level);
        this.entityType = parent.entityType;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.SIZE = size;
        xpReward = 100;
        setCustomName(new TextComponent("Queen Buzzlet"));
        setCustomNameVisible(true);
        //setNoGravity(true);
        this.navigation.setCanFloat(true);
        this.event = parent.event;
        if(size == 1) {
            this.setHealth(parent.getHealth() / 4);
        }
        else if(size == 2){
            this.setHealth(parent.getHealth() / 2);
        }
        //set this to false to enable AI. I was using this to adjust the hit box and the model
        //setNoAi(false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return (double) (5.0 + entity.getBbWidth() * entity.getBbWidth());
            }
        });

        this.goalSelector.addGoal(0, new StingerAttackGoal<>(this, 1.0D, 30, 15));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(4, new RandomFlyGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 2){
            @Override
            public boolean canUse() {
                bossInfo.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is trying Panicking"), ChatType.CHAT, p.getUUID()));});

                return ((QueenBuzzlet)this.mob).phase == QueenBuzzletPhase.QUARTER && super.canUse();
            }

            @Override
            public void start() {
                bossInfo.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is Panicking"), ChatType.CHAT, p.getUUID()));});
                super.start();
            }
        });
    }

    public Set<QueenBuzzlet> split(int size){
        Set<QueenBuzzlet> children = Sets.newHashSet();
        children.add(new QueenBuzzlet(this, size));
        if(size == 1){
            children.add(new QueenBuzzlet(this, size));
            children.add(new QueenBuzzlet(this, size));
            children.add(new QueenBuzzlet(this, size));
        }
        for (QueenBuzzlet child : children) {
            child.moveTo(this.getX(), this.getY() + 0.5D, this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
            child.event = null;
            child.isOwnerOfEvent = false;
            child.setHealth(size == 1 ? this.getHealth() / 4 : this.getHealth() / 2);
            child.flyingSpeed = size == 1 ? 0.5f : 0.15F;
            this.level.addFreshEntity(child);
        }

        return children;
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

    @Override
    public boolean causeFallDamage(float funny, float numbers, DamageSource meansNothing){
        return false;
    }

    @Override
    protected void checkFallDamage(double these, boolean numbers, BlockState mean, BlockPos nothing) {
    }

    //this is the method that is called when Queen Buzzlet reaches half health
    //if Queen Buzzlet is at half health, it will split into two Queen Buzzlets
    //each Queen Buzzlet will share the same BossEvent, but have their own amount of health
    //the BossEvent will be updated to reflect the new health of both Queen Buzzlets combined

//    Currently, this is copied from the Slime class
//    protected void setSize(int p_33594_, boolean p_33595_) {
//        int i = Mth.clamp(p_33594_, 1, 127);
//        this.entityData.set(ID_SIZE, i);
//        this.reapplyPosition();
//        this.refreshDimensions();
//        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getHealth()/2);
//        if (p_33595_) {
//            this.setHealth(this.getMaxHealth());
//        }
//        this.xpReward = i;
//    }

    public @NotNull EntityType<? extends QueenBuzzlet> getType() {
        return (EntityType<? extends QueenBuzzlet>)super.getType();
    }


    // During my research I found a bunch of methods you guys may want to use
    // This first one is obviously for registering goals for the ai to use

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
    public @NotNull MobType getMobType(){
        return MobType.UNDEFINED;
    }
    //This is a method that is triggered when the player first sees the entity
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if(this.isOwnerOfEvent){
            event.addPlayer(player);
        }

    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player){
        super.stopSeenByPlayer(player);
        if(this.isOwnerOfEvent){
            event.removePlayer(player);
        }
    }

    @Override
    public void customServerAiStep(){
        super.customServerAiStep();
        //this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void tick(){
        super.tick();
        if(this.level.getNearestPlayer(this, 50) == null){
            if(getHealth() < getMaxHealth())
                this.heal(0.1f);
        }


        if (this.isOwnerOfEvent) {
            event.tick();
            if(this.getHealth() < 20) {
                if (!event.canOwnerBeRemoved()){
                    this.dead = false;
                    this.setHealth(21);
                } else{
                    event.endEvent();
                }
            }
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
        builder = builder.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED);//1f
        builder = builder.add(Attributes.MAX_HEALTH, MAX_HEALTH);//300
        builder = builder.add(Attributes.ARMOR, ARMOR);//0
        builder = builder.add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);//3
        builder = builder.add(Attributes.FLYING_SPEED, FLYING_SPEED);//2f
        builder = builder.add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);//25
        return builder;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    //This Method was taken from net.minecraft.world.entity.animal.Bee;
    @Override
    protected PathNavigation createNavigation(Level p_27815_) {
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

    //this class is used to split the Queen Buzzlet into two when it reaches half health
    //it is itself a QueenBuzzlet, and is spawned as a child of the parent entity
    //the child entity will have the same attributes as the parent, but will have half the health
    //the constructor for the child entity will take in the parent entity as a parameter
    //the parent entity will be removed from the world, and the child entities will be added to the world
    //the child entities will have the same position and rotation as the parent entity
    //the child entities will have the same BossEvent as the parent entity
    //the child entities will each have half the same health as the parent entity
//    public class QueenBuzzletHalfChild extends QueenBuzzlet {
//        protected final ServerBossEvent bossInfo;
//        public QueenBuzzletHalfChild(QueenBuzzlet queenBuzzlet) {
//            super((EntityType<QueenBuzzlet>) queenBuzzlet.getType(), queenBuzzlet.getLevel());
//            this.setHealth(queenBuzzlet.getHealth() / 2);
//            this.bossInfo = queenBuzzlet.bossInfo;
//            this.setBoundingBox(queenBuzzlet.getBoundingBox());
//            this.setPos(queenBuzzlet.getX(), queenBuzzlet.getY(), queenBuzzlet.getZ());
//            this.setRot(queenBuzzlet.getXRot(), queenBuzzlet.getYRot());
//            for(WrappedGoal goal : queenBuzzlet.goalSelector.getRunningGoals().toList()){
//                this.goalSelector.addGoal(goal.getPriority(), goal.getGoal());
//            }
//            for(WrappedGoal goal : queenBuzzlet.targetSelector.getRunningGoals().toList()){
//                this.targetSelector.addGoal(goal.getPriority(), goal.getGoal());
//            }
//        }
//
//        Raid
//
//        public static AttributeSupplier.Builder createAttributes(){
//            AttributeSupplier.Builder builder = Mob.createMobAttributes();
//            builder = builder.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED * 2);
//            builder = builder.add(Attributes.MAX_HEALTH, MAX_HEALTH / 2);
//            builder = builder.add(Attributes.ARMOR, ARMOR);
//            builder = builder.add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE / 2);
//            builder = builder.add(Attributes.FLYING_SPEED, FLYING_SPEED * 2);
//            builder = builder.add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
//            return builder;
//        }
//
//        @Override
//        public void tick(){
//            super.tick();
//            if(this.level.getNearestPlayer(this, 50) == null){
//                if(getHealth() < getMaxHealth())
//                    this.heal(0.1f);
//            }
//        }
//    }



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
        //private int cooldown = 0;

        RandomFlyGoal(){
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
//            if(cooldown <= 0) {
//                return true;
//            }
//            else{
//                cooldown--;
//                return false;
//            }
            return true;
        }

        @Override
        public boolean canContinueToUse(){
            return QueenBuzzlet.this.navigation.isInProgress();
        }

        @Override
        public void start(){
            Vec3 randomPosition = this.findPos();
            if(randomPosition != null) {
                QueenBuzzlet.this.navigation.moveTo(QueenBuzzlet.this.navigation.createPath(new BlockPos(randomPosition), 3), 1.0D);
                //cooldown = 10;
            }
        }

        private Vec3 findPos(){
            Vec3 position = QueenBuzzlet.this.getViewVector(0.0F + new Random().nextFloat(-1.0F, 1.0F));
            Vec3 randomPosition = HoverRandomPos.getPos(QueenBuzzlet.this, 8, 7, position.x, position.z, ((float)Math.PI / 3F), 3, 1);
            return randomPosition != null ? randomPosition : AirAndWaterRandomPos.getPos(QueenBuzzlet.this, 8, 4, -2, position.x, position.z, (double)((float)Math.PI / 2F));
        }

    }

    public class GoHomeGoal extends MoveToBlockGoal{

        Block home = Blocks.POPPY;

        public GoHomeGoal(PathfinderMob mob, double speedModifier, int searchRange, int verticalSearchRange) {
            super(mob, speedModifier, searchRange, verticalSearchRange);
        }

        @Override
        protected boolean isValidTarget(@NotNull LevelReader p_25619_, @NotNull BlockPos p_25620_) {
            if(this.mob.getTarget() != null) {
                return p_25619_.getBlockState(p_25620_).is(home) &&
                        (Math.abs(p_25620_.getX() - this.mob.getTarget().getX()) >= 64 || Math.abs(p_25620_.getZ() - this.mob.getTarget().getZ()) >= 64);
            }
            return false;
        }
    }
}
