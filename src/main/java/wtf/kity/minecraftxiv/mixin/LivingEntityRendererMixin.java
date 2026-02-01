package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {
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
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        LocalPlayer player = Minecraft.getInstance().player;

        if (livingEntity.is(player)
                && camera.isDetached()
                && camera.getPosition().distanceTo(player.getEyePosition()) < 1.0) {
            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            vertexConsumer = multiBufferSource.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(livingEntity)));
            h = 0.15f;
        }

        model.renderToBuffer(poseStack, vertexConsumer, i, j, f, g, h, k);
    }
}
