package com.fcs.locomotionmobs.client.render;


import com.fcs.locomotionmobs.Main;
import com.fcs.locomotionmobs.client.model.QueenBuzzletModel;
import com.fcs.locomotionmobs.entities.QueenBuzzlet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

//This is what links the entity to the model
public class QueenBuzzletRenderer extends MobRenderer<QueenBuzzlet, QueenBuzzletModel<QueenBuzzlet>> {
    //This is the location of the texture for Queen Buzzlet. It's used in other places to grab the texture
    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/entity/queen_buzzlet.png");

    //this constructor is used to link the texture to the model. the float is the size of the shadow!
    public QueenBuzzletRenderer(EntityRendererProvider.Context context) {
        super(context, new QueenBuzzletModel(context.bakeLayer(QueenBuzzletModel.LAYER_LOCATION)), 0.5f);
    }

    //When things are needy and use a method to ask for TEXTURE
    @Override
    public ResourceLocation getTextureLocation(QueenBuzzlet queenBuzzlet) {
        return TEXTURE;
    }
}
