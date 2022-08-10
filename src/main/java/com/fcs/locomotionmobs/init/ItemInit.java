package com.fcs.locomotionmobs.init;

import com.fcs.locomotionmobs.Main;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

//This is where Items are Initialized
public class ItemInit {
    //This creates the registry
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    //This is us adding the spawn egg to the registry
    public static final RegistryObject<Item> QUEEN_BUZZLET_SPAWN_EGG = ITEMS.register("queen_buzzlet_spawn_egg",
            () ->
                    new ForgeSpawnEggItem(EntityInit.QUEEN_BUZZLET,//You can change the next 2 parameters to change the color of the egg
                            2, 88,//the next parameter tells minecraft where to put the egg in the creative menu
                            new Item.Properties().tab(ModCreativeTab.instance)));


    //This class is for making our own tab in the creative menu
    public static class ModCreativeTab extends CreativeModeTab{
        //This adds a tab to the end of all the existing tabs called Locomotion Mobs
        public static final ModCreativeTab instance = new ModCreativeTab(CreativeModeTab.TABS.length, "Locomotion Mobs");

        private ModCreativeTab(int index, String label){
            super(index, label);
        }

        //This sets what is uses for the icon in the creative tab
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(QUEEN_BUZZLET_SPAWN_EGG.get());
        }
    }
}
