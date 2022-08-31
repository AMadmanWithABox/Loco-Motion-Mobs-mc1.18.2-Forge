package com.fcs.locomotionmobs.client.render;

import com.fcs.locomotionmobs.Main;
import com.fcs.locomotionmobs.entities.QueenBuzzletStinger;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class QueenBuzzletStingerRenderer extends ArrowRenderer<QueenBuzzletStinger> {

    //This is the location of the texture for Queen Buzzlet's Stinger. It's used in other places to grab the texture
    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/items/queen_buzzlet_stinger.png");

    public QueenBuzzletStingerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    //When things are needy and use a method to ask for TEXTURE
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull QueenBuzzletStinger stinger) {
        return TEXTURE;
    }
}
