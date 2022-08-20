package com.fcs.locomotionmobs.client.model;// Made with Blockbench 4.3.1
// Exported for Minecraft version 1.17 - 1.18 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.fcs.locomotionmobs.entities.QueenBuzzlet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class QueenBuzzletModel<T extends QueenBuzzlet> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("locomotionmobs", "queen_buzzlet"), "main");
	private final ModelPart head;
	private final ModelPart StabbyPiece;
	private final ModelPart Chest;
	private final ModelPart Thorax;
	private final ModelPart Wings;
	private final ModelPart LegMiddleLeft;
	private final ModelPart LegMiddleRight;
	private final ModelPart LegFrontRight;
	private final ModelPart LegFrontLeft;
	private final ModelPart LegBackLeft;
	private final ModelPart LegBackRight;

	public QueenBuzzletModel(ModelPart root) {
		this.head = root.getChild("head");
		this.StabbyPiece = root.getChild("StabbyPiece");
		this.Chest = root.getChild("Chest");
		this.Thorax = root.getChild("Thorax");
		this.Wings = root.getChild("Wings");
		this.LegMiddleLeft = root.getChild("LegMiddleLeft");
		this.LegMiddleRight = root.getChild("LegMiddleRight");
		this.LegFrontRight = root.getChild("LegFrontRight");
		this.LegFrontLeft = root.getChild("LegFrontLeft");
		this.LegBackLeft = root.getChild("LegBackLeft");
		this.LegBackRight = root.getChild("LegBackRight");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 15).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(0, 9).addBox(-2.5F, 1.0F, -6.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(0, 27).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(7, -4).addBox(2.0F, -5.0F, -9.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(15, -4).addBox(-2.0F, -5.0F, -9.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition Eye1 = head.addOrReplaceChild("Eye1", CubeListBuilder.create().texOffs(11, 2).addBox(-4.5F, -24.0F, -18.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(6, 4).addBox(-3.5F, -23.0F, -18.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(1, 5).addBox(-2.5F, -22.0F, -18.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(20, 6).addBox(-4.5F, -23.0F, -15.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 21.0F, 12.0F));

		PartDefinition Eye2 = head.addOrReplaceChild("Eye2", CubeListBuilder.create().texOffs(11, 2).addBox(-2.5F, -24.0F, -18.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(6, 4).addBox(-3.5F, -23.0F, -18.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(1, 5).addBox(-4.5F, -22.0F, -18.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(20, 6).addBox(-2.5F, -23.0F, -15.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, 21.0F, 12.0F));

		PartDefinition StabbyPiece = partdefinition.addOrReplaceChild("StabbyPiece", CubeListBuilder.create().texOffs(110, 2).addBox(-1.5F, 0.0F, 5.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(122, 3).addBox(-0.5F, 1.0F, 8.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(48, 31).addBox(-3.5F, -3.0F, -2.0F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, 22.0F));

		PartDefinition Abdomen = StabbyPiece.addOrReplaceChild("Abdomen", CubeListBuilder.create().texOffs(62, 0).addBox(-6.0F, -1.1063F, -6.2155F, 12.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -11.0F, 1.7017F, 0.0F, 0.0F));

		PartDefinition Chest = partdefinition.addOrReplaceChild("Chest", CubeListBuilder.create().texOffs(24, 0).addBox(-5.0F, -4.1368F, -22.0134F, 10.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.0F, 3.0F, 1.1345F, 0.0F, 0.0F));

		PartDefinition Thorax = partdefinition.addOrReplaceChild("Thorax", CubeListBuilder.create().texOffs(24, 21).addBox(-3.0F, -1.1063F, -5.2155F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition Wings = partdefinition.addOrReplaceChild("Wings", CubeListBuilder.create().texOffs(83, 22).addBox(2.0F, 0.0F, -1.0F, 11.0F, 0.0F, 23.0F, new CubeDeformation(0.0F))
				.texOffs(61, 22).addBox(-13.0F, 0.0F, -1.0F, 11.0F, 0.0F, 23.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -4.0F));

		PartDefinition LegMiddleLeft = partdefinition.addOrReplaceChild("LegMiddleLeft", CubeListBuilder.create().texOffs(48, 22).addBox(0.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 27).addBox(7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 10.0F, 0.0F));

		PartDefinition LegMiddleRight = partdefinition.addOrReplaceChild("LegMiddleRight", CubeListBuilder.create().texOffs(48, 22).addBox(-7.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 27).addBox(-18.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 10.0F, 0.0F));

		PartDefinition LegFrontRight = partdefinition.addOrReplaceChild("LegFrontRight", CubeListBuilder.create().texOffs(48, 22).addBox(-7.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 27).addBox(-18.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 6.0F, -6.0F));

		PartDefinition LegFrontLeft = partdefinition.addOrReplaceChild("LegFrontLeft", CubeListBuilder.create().texOffs(48, 22).addBox(0.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 27).addBox(7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 6.0F, -6.0F));

		PartDefinition LegBackLeft = partdefinition.addOrReplaceChild("LegBackLeft", CubeListBuilder.create().texOffs(48, 22).addBox(0.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 27).addBox(7.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 6.0F, 7.0F));

		PartDefinition LegBackRight = partdefinition.addOrReplaceChild("LegBackRight", CubeListBuilder.create().texOffs(48, 22).addBox(-7.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 27).addBox(-18.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 6.0F, 6.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		StabbyPiece.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Chest.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Thorax.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Wings.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LegMiddleLeft.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LegMiddleRight.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LegFrontRight.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LegFrontLeft.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LegBackLeft.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LegBackRight.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}