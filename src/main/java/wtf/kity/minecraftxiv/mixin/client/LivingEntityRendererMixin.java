package wtf.kity.minecraftxiv.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? <26 {
/*import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
*///? } else {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Shadow;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
//? }

@Mixin(LivingEntityRenderer.class)
//? <26 {
/*public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {
    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            )
    )
    public void renderToBuffer(
            M model,
            PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, float f, float g, float h, float k,
            @Local(argsOnly = true) T livingEntity,
            @Local(argsOnly = true) MultiBufferSource multiBufferSource
    ) {
*///? } else {
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, SS> {
    @Shadow
    public abstract Identifier getTextureLocation(S state);
    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    public void render(
            SubmitNodeCollector instance,
            Model<? super SS> model,
            SS state,
            PoseStack matrices,
            RenderType renderLayer,
            int light,
            int overlay,
            int tintedColor,
            @Nullable TextureAtlasSprite sprite,
            int outlineColor,
            @Nullable CrumblingOverlay crumblingOverlay,
            @Local(argsOnly = true) S livingEntityRenderState
    ) {
//? }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        LocalPlayer player = Minecraft.getInstance().player;

        //? <26 {
        /*if (livingEntity.is(player)
        *///? } else {
        if (livingEntityRenderState instanceof AvatarRenderState playerEntityRenderState
                && player != null
                && playerEntityRenderState.id == player.getId()
        //? }
                && camera.isDetached()
                //? <26 {
                /*&& camera.getPosition().distanceTo(player.getEyePosition()) < 1.0) {
                *///? } else {
                && camera.position().distanceTo(player.getEyePosition()) < 1.0) {
            //? }

            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            //? <26 {
            /*vertexConsumer = multiBufferSource.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(livingEntity)));
            h = 0.15f;
            *///? } else {
            renderLayer = RenderTypes.itemEntityTranslucentCull(this.getTextureLocation((S) state));
            tintedColor = 0x26FFFFFF;
            //? }
        }

        //? <26 {
        /*model.renderToBuffer(poseStack, vertexConsumer, i, j, f, g, h, k);
        *///? } else {
        instance.submitModel(model, state, matrices, renderLayer, light, overlay, tintedColor, sprite, outlineColor, crumblingOverlay);
        //? }
    }
}
