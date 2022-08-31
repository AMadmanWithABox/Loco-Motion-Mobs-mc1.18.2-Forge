package com.fcs.locomotionmobs.entities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StingerItem extends Item {

    public StingerItem(Properties p_41383_) {
        super(p_41383_);
    }

    public QueenBuzzletStinger createStinger(Level p_40513_, ItemStack p_40514_, LivingEntity p_40515_) {
        //stinger.setEffectsFromItem(p_40514_);
        return new QueenBuzzletStinger(p_40513_, p_40515_);
    }
}
