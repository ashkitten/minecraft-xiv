package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
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
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        LocalPlayer player = Minecraft.getInstance().player;

        if (livingEntityRenderState instanceof AvatarRenderState playerEntityRenderState
                && player != null
                && playerEntityRenderState.id == player.getId()
                && camera.isDetached()
                && camera.position().distanceTo(player.getEyePosition()) < 1.0) {
            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            renderLayer = RenderTypes.itemEntityTranslucentCull(this.getTextureLocation((S) state));
            tintedColor = 0x26FFFFFF;
        }

        instance.submitModel(model, state, matrices, renderLayer, light, overlay, tintedColor, sprite, outlineColor, crumblingOverlay);
    }
}
