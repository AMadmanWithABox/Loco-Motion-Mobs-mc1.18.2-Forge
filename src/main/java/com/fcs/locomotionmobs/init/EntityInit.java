package com.fcs.locomotionmobs.init;

import com.fcs.locomotionmobs.Main;
import com.fcs.locomotionmobs.entities.QueenBuzzlet;
import com.fcs.locomotionmobs.entities.QueenBuzzletStinger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityInit {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MOD_ID);
    public static final RegistryObject<EntityType<QueenBuzzlet>> QUEEN_BUZZLET = register("queen_buzzlet",
            EntityType.Builder.<QueenBuzzlet>of(QueenBuzzlet::new, MobCategory.MONSTER)
                .setShouldReceiveVelocityUpdates(true)
                .sized(2.5F, 2.0F)
                .clientTrackingRange(50)
                .setCustomClientFactory(QueenBuzzlet::new));

    public static final RegistryObject<EntityType<QueenBuzzletStinger>> QUEEN_BUZZLET_STINGER = ENTITIES.register("queen_buzzlet_stinger",
            () -> EntityType.Builder.of((EntityType.EntityFactory<QueenBuzzletStinger>) QueenBuzzletStinger::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .build("queen_buzzlet_stinger"));

    //My final problem was everything below here. The game did not know what the attributes of the mob were and therefore could not spawn it.

    //Here we are building each registry object as we go through them
    private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryName, EntityType.Builder<T> entityTypeBuilder) {
        return ENTITIES.register(registryName, () -> entityTypeBuilder.build(registryName));
    }

    //here we are calling init on the QueenBuzzlet class. This will allow QueenBuzzlet to spawn naturally
//    @SubscribeEvent
//    public static void init(FMLCommonSetupEvent event){
//        event.enqueueWork(QueenBuzzlet::init);
//    }


    //This is the really important one. Without this, Queen Buzzlet will not spawn. Entities require attributes to spawn
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event){
        event.put(QUEEN_BUZZLET.get(), QueenBuzzlet.createAttributes().build());
    }
}
