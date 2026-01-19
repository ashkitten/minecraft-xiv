package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, SS> {
    @Shadow
    public abstract Identifier getTexture(S state);

    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            )
    )
    public void render(
            OrderedRenderCommandQueue instance,
            Model<? super SS> model,
            SS state,
            MatrixStack matrices,
            RenderLayer renderLayer,
            int light,
            int overlay,
            int tintedColor,
            @Nullable Sprite sprite,
            int outlineColor,
            @Nullable CrumblingOverlayCommand crumblingOverlay,
            @Local(argsOnly = true) S livingEntityRenderState
    ) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (livingEntityRenderState instanceof PlayerEntityRenderState playerEntityRenderState
                && player != null
                && playerEntityRenderState.id == player.getId()
                && camera.isThirdPerson()
                && camera.getCameraPos().distanceTo(player.getEyePos()) < 1.0) {
            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            renderLayer = RenderLayers.itemEntityTranslucentCull(this.getTexture((S) state));
            tintedColor = 0x26FFFFFF;
        }

        instance.submitModel(model, state, matrices, renderLayer, light, overlay, tintedColor, sprite, outlineColor, crumblingOverlay);
    }
}
