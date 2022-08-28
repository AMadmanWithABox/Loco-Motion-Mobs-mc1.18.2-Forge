package com.fcs.locomotionmobs.entities;

import com.fcs.locomotionmobs.init.EntityInit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;

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
        super(EntityType.ARROW, p_36867_, p_36866_);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
