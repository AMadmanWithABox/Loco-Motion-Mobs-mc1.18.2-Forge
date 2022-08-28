package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.EnumSet;
import java.util.Random;


public class QueenBuzzlet extends Monster implements FlyingAnimal, RangedAttackMob {
    protected static final float MOVEMENT_SPEED = 0.1F;
    protected static final float FLYING_SPEED = 1F;
    protected static final float MAX_HEALTH = 100;
    protected static final float ATTACK_DAMAGE = 3;
    protected static final float ARMOR = 1;
    protected static final float FOLLOW_RANGE = 25;

    private static final Component QUEEN_BUZZLET_EVENT_TITLE = new TextComponent("Queen Buzzlet");

    private final ServerBossEvent bossEvent = new ServerBossEvent(QUEEN_BUZZLET_EVENT_TITLE,
            ServerBossEvent.BossBarColor.PURPLE,
            ServerBossEvent.BossBarOverlay.NOTCHED_6);
    private QueenBuzzletPhase phase;

    @Override
    public void performRangedAttack(LivingEntity p_33317_, float p_33318_) {
        ThrownPotion potion = new ThrownPotion(this.level, this);
        Vec3 vec3 = p_33317_.getDeltaMovement();
        double d0 = p_33317_.getX() + vec3.x - this.getX();
        double d1 = p_33317_.getEyeY() - (double)1.1F - this.getY();
        double d2 = p_33317_.getZ() + vec3.z - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        potion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_POISON));
        potion.setXRot(potion.getXRot() - -20.0F);
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
        this.moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 100;
        setCustomName(new TextComponent("Queen Buzzlet"));
        setCustomNameVisible(true);
        this.navigation.setCanFloat(true);

    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return (double) (5.0 + entity.getBbWidth() * entity.getBbWidth());
            }
        });

        this.targetSelector.addGoal(1, new StingerAttackGoal<>(this, 1.0D, 10, 25));
        this.targetSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 20, 40, 15));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RandomFlyGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 2) {
            @Override
            public boolean canUse() {
                bossEvent.getPlayers().forEach(p -> {
                    p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is trying Panicking"), ChatType.CHAT, p.getUUID()));
                });

                return ((QueenBuzzlet)this.mob).phase == QueenBuzzletPhase.QUARTER && super.canUse();
            }

            @Override
            public void start() {
                bossEvent.getPlayers().forEach(p -> {
                    p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is Panicking"), ChatType.CHAT, p.getUUID()));
                });
                super.start();
            }
        });

        //this.goalSelector.addGoal(0, new RandomFlyGoal());
        this.targetSelector.addGoal(1, new QueenBuzzletSweepAttackGoal());
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

    @Override
    public SoundEvent getAmbientSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.bee.loop"));
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
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
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);

    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player){
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
        if(this.level.getNearestPlayer(this, 50) == null){
            if(getHealth() < getMaxHealth())
                this.heal(0.1f);
        }
        if(((this.getHealth() <= getMaxHealth() / 2) && (this.getHealth() > getMaxHealth() / 4)) && (this.phase != QueenBuzzletPhase.HALF)){
            this.phase = QueenBuzzletPhase.HALF;
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is at phase Half"), ChatType.CHAT, p.getUUID()));});
            //this.flyingSpeed = 0.2F;
        } else if ((this.getHealth() <= getMaxHealth() / 4) && (this.phase != QueenBuzzletPhase.QUARTER)){
            this.phase = QueenBuzzletPhase.QUARTER;
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet at phase Quarter"), ChatType.CHAT, p.getUUID()));});
            //this.flyingSpeed = 0.5F;
        } else if(this.getHealth() > this.getMaxHealth() / 2 && this.phase != QueenBuzzletPhase.FULL){
            this.phase = QueenBuzzletPhase.FULL;
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is Full"), ChatType.CHAT, p.getUUID()));});
            //this.flyingSpeed = 0.1F;
        }
    }

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


    public class StingerAttackGoal<T extends Mob & RangedAttackMob> extends RangedBowAttackGoal{
        //used to simulate charging a shot. this is just the timer
        private int attackTime = -1;
        //this defines what an arrow is
        //private StingerItem stinger = new StingerItem((new Item.Properties()).tab(CreativeModeTab.TAB_COMBAT));
        private ArrowItem stinger = (ArrowItem)Items.ARROW;
        //setting up a cooldown timer so Queen Buzzlet doesn't just spam arrows
        private int cooldownTimer = 0;
        //yes game, Queen Buzzlet totally has "Arrows" in its inventory
        private ItemStack stingers = new ItemStack(() -> stinger);
        private QueenBuzzlet usingEntity;
        private final int attackInterval;
        public StingerAttackGoal(QueenBuzzlet usingEntity, double speedMod, int attackInterval, float attackRadius) {
            super(usingEntity, speedMod, attackInterval, attackRadius);
            this.attackInterval = attackInterval;
            this.usingEntity = usingEntity;
        }

        @Override
        public boolean canUse() {
            if(usingEntity.phase != QueenBuzzletPhase.QUARTER) {
                //check the cooldown timer to use
                if (cooldownTimer == 0) {
                    return true;
                } else {
                    cooldownTimer--;
                    return false;
                }
            } else {
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

            if (cooldown <= 0) {
                //cooldown--;
                return true;
            } else {
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
            Vec3 setRealZero = new Vec3(0.0D, 10.0D, 0.0D);
            if (randomPosition != null) {
                QueenBuzzlet.this.navigation.moveTo(QueenBuzzlet.this.navigation.createPath(new BlockPos(randomPosition.subtract(setRealZero)), 3), 1.0D);
                //cooldown = 10;
            }
        }

        private Vec3 findPos() {
            Vec3 position = QueenBuzzlet.this.getViewVector(0.0F + new Random().nextFloat(-1.0F, 1.0F));
            Vec3 randomPosition = HoverRandomPos.getPos(QueenBuzzlet.this, 8, 7, position.x, position.z, ((float) Math.PI / 3F), 3, 1);
            return randomPosition != null ? randomPosition : AirAndWaterRandomPos.getPos(QueenBuzzlet.this, 8, 4, -2, position.x, position.z, (double) ((float) Math.PI / 2F));
        }

    }

    public class GoHomeGoal extends MoveToBlockGoal {

        Block home = Blocks.POPPY;

        public GoHomeGoal(PathfinderMob mob, double speedModifier, int searchRange, int verticalSearchRange) {
            super(mob, speedModifier, searchRange, verticalSearchRange);
        }

        @Override
        protected boolean isValidTarget(@NotNull LevelReader p_25619_, @NotNull BlockPos p_25620_) {
            if (this.mob.getTarget() != null) {
                return p_25619_.getBlockState(p_25620_).is(home) &&
                        (Math.abs(p_25620_.getX() - this.mob.getTarget().getX()) >= 64 || Math.abs(p_25620_.getZ() - this.mob.getTarget().getZ()) >= 64);
            }
            return false;
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
                if (livingentity instanceof Player) {
                    Player player = (Player) livingentity;
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
                if (QueenBuzzlet.this.getBoundingBox().inflate((double) 0.2F).intersects(livingentity.getBoundingBox())) {
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
}


