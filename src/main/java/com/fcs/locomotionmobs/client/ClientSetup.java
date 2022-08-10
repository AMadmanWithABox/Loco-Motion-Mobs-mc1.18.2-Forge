package com.fcs.locomotionmobs.client;

import com.fcs.locomotionmobs.Main;
import com.fcs.locomotionmobs.client.model.QueenBuzzletModel;
import com.fcs.locomotionmobs.client.render.QueenBuzzletRenderer;
import com.fcs.locomotionmobs.init.EntityInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

//For some reason, you need to register textures and renderers for entities. If we add any other mobs, we will need
//to add some code here
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    //registers textures
    @SubscribeEvent
    public static void registerEntityTextures(EntityRenderersEvent.RegisterLayerDefinitions event){
        //this method has documentation if you hover over it so I will not explain
        event.registerLayerDefinition(QueenBuzzletModel.LAYER_LOCATION, QueenBuzzletModel::createBodyLayer);
    }
    //registers renderers
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event){
        //Same as above^^
        event.registerEntityRenderer(EntityInit.QUEEN_BUZZLET.get(), QueenBuzzletRenderer::new);
    }
}
