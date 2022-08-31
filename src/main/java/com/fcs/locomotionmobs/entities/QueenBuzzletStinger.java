package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.init.EntityInit;
import com.fcs.locomotionmobs.init.ItemInit;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class QueenBuzzletStinger extends AbstractArrow {

    public QueenBuzzletStinger(EntityType<QueenBuzzletStinger> entityType, Level world) {
        super(entityType, world);
    }

    public QueenBuzzletStinger(EntityType<QueenBuzzletStinger> entityType, double x, double y, double z, Level world) {
        super(entityType, x, y, z, world);
    }

    public QueenBuzzletStinger(EntityType<QueenBuzzletStinger> entityType, LivingEntity shooter, Level world) {
        super(entityType, shooter, world);
    }

    public QueenBuzzletStinger(Level p_36866_, LivingEntity p_36867_) {
        super(EntityInit.QUEEN_BUZZLET_STINGER.get(), p_36867_, p_36866_);
    }

    protected @NotNull ItemStack getPickupItem() {
        return new ItemStack(ItemInit.QUEEN_BUZZLET_STINGER.get());
    }

    @Override
    public @NotNull Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

}
