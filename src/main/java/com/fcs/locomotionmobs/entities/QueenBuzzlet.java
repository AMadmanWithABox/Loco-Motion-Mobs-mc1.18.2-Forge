package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.init.EntityInit;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.VisibleForDebug;
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
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
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
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.EnumSet;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class QueenBuzzlet extends Monster implements FlyingAnimal {

    QueenBuzzlet.QueenGoToHiveGoal goToBaseGoal;
    protected static final float MOVEMENT_SPEED = 1F;
    protected static final float FLYING_SPEED = 2F;
    protected static final float MAX_HEALTH = 300;
    protected static final float ATTACK_DAMAGE = 3;
    protected static final float ARMOR = 1;
    protected static final float FOLLOW_RANGE = 25;

    private static final Component QUEEN_BUZZLET_EVENT_TITLE = new TextComponent("Queen Buzzlet");

    private final ServerBossEvent bossEvent = new ServerBossEvent(QUEEN_BUZZLET_EVENT_TITLE,
            ServerBossEvent.BossBarColor.PURPLE,
            ServerBossEvent.BossBarOverlay.NOTCHED_6);
    private QueenBuzzletPhase phase;


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

    private enum QueenBuzzletPhase {
        FULL, HALF, QUARTER
    }

    public QueenBuzzlet(PlayMessages.SpawnEntity packet, Level world) {
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
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RandomFlyGoal());
        this.goalSelector.addGoal(0, new QueenBuzzlet.QueenLocateHiveGoal());
        this.goToBaseGoal = new QueenBuzzlet.QueenGoToHiveGoal();
        this.goalSelector.addGoal(0, this.goToBaseGoal);
        this.goalSelector.addGoal(2, new PanicGoal(this, 2) {
            @Override
            public boolean canUse() {
                bossEvent.getPlayers().forEach(p -> {
                    p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is trying Panicking"), ChatType.CHAT, p.getUUID()));
                });

                return ((QueenBuzzlet) this.mob).phase == QueenBuzzletPhase.QUARTER && super.canUse();
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
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void checkDespawn() {
        if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            discard();
        }
    }

    @Override
    public boolean causeFallDamage(float funny, float numbers, DamageSource meansNothing) {
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
        return (EntityType<? extends QueenBuzzlet>) super.getType();
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
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void tick() {
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


    public class StingerAttackGoal<T extends Mob & RangedAttackMob> extends RangedBowAttackGoal {
        //used to simulate charging a shot. this is just the timer
        private int attackTime = -1;
        //this defines what an arrow is
        private ArrowItem stinger = (ArrowItem) Items.ARROW;

        //setting up a cooldown timer so Queen Buzzlet doesn't just spam arrows
        private int cooldownTimer = 5;
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
            if (attackTime > attackInterval) {
                AbstractArrow abstractStinger = stinger.createArrow(usingEntity.getLevel(), stingers, usingEntity);

                //the fifth parameter adjusts power of the stinger
                //abstractStinger.shootFromRotation(usingEntity, usingEntity.getXRot(), usingEntity.getYRot(), 0.0F, 3, 1.0F);
                //the damage dealt by the stinger
                abstractStinger.setBaseDamage(2.0D);
                //knockback of the stinger
                abstractStinger.setKnockback(2);
                if (target != null && target.isAlive()) {
                    usingEntity.getLookControl().setLookAt(target);
                    if (usingEntity.getLookControl().isLookingAtTarget()) {
                        //These 4 variables get the distances to pass into the shoot method
                        //From here to abstractStinger.shoot(double, double, double, float, float) is from the Skeleton class
                        double targetX = target.getX() - usingEntity.getX();
                        double targetY = target.getY((1.0D / 3.0D)) - abstractStinger.getY();
                        double targetZ = target.getZ() - usingEntity.getZ();
                        double targetLine = Math.sqrt((targetX * targetX) + (targetZ * targetZ));
                        //The 4th parameter seems to set the damage, and my best estimate of the 5th is some sort of scaling based on difficulty (maybe accuracy)
                        abstractStinger.shoot(targetX, targetY + targetLine * 0.2D, targetZ, 2.0F, (float)
                                (14 - usingEntity.level.getDifficulty().getId() * 4));

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
        public boolean canContinueToUse() {
            return QueenBuzzlet.this.navigation.isInProgress();
        }

        @Override
        public void start() {
            Vec3 randomPosition = this.findPos();
            Vec3 setRealZero = new Vec3(0.0D, 10.0D, 0.0D);
            if (randomPosition != null) {
                QueenBuzzlet.this.navigation.moveTo(QueenBuzzlet.this.navigation.createPath(new BlockPos(randomPosition.subtract(setRealZero)),
                        3), 1.0D);
                //cooldown = 10;
            }
        }

        private Vec3 findPos() {
            Vec3 position = QueenBuzzlet.this.getViewVector(0.0F + new Random().nextFloat(-1.0F, 1.0F));
            Vec3 randomPosition = HoverRandomPos.getPos(QueenBuzzlet.this, 8, 7, position.x,
                    position.z, ((float) Math.PI / 3F), 3, 1);
            return randomPosition != null ? randomPosition : AirAndWaterRandomPos.getPos(QueenBuzzlet.this, 8,
                    4, -2, position.x, position.z, (double) ((float) Math.PI / 2F));
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



