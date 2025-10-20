package wtf.kity.minecraftxiv.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.mod.Mod;

import java.lang.reflect.Field;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Redirect(
            method = "renderCrosshair", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"
    )
    )
    private boolean isFirstPerson(Perspective perspective) {
        if (perspective.isFirstPerson()) return true;
        if (!Mod.enabled) return false;
        // render crosshair unless move-camera binding is pressed and we're not locked on
        return !ClientInit.moveCameraBinding.isPressed() || Mod.lockOnTarget != null;
    }

    @Inject(
            method = "renderCrosshair", at = @At("HEAD")
    )
    private void crosshairPre(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            double scaleFactor = client.getWindow().getScaleFactor();
            Mouse mouse = client.mouse;

            Camera camera = client.gameRenderer.getCamera();

            MatrixStack matrixStack = context.getMatrices();
            matrixStack.push();
            if (Mod.lockOnMouseTarget != null) {
                matrixStack.translate(camera.getPos().subtract(Mod.lockOnMouseTarget));
                Quaternionf quat = new Quaternionf();
                quat.rotateX(-camera.getPitch() * (float) (Math.PI / 180.0));
//                quat.rotateY(camera.getYaw() * (float) (Math.PI / 180.0));
                matrixStack.multiply(quat);
            }
            matrixStack.translate(
                    -context.getScaledWindowWidth() / 2.0 + mouse.getX() / scaleFactor,
                    -context.getScaledWindowHeight() / 2.0 + mouse.getY() / scaleFactor,
                    0.0
            );

//
//            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
//            matrix4fStack.pushMatrix();
//            matrix4fStack.mul(context.getMatrices().peek().getPositionMatrix());
//            matrix4fStack.translate(context.getScaledWindowWidth() / 2, context.getScaledWindowHeight() / 2, 0.0F);
//            matrix4fStack.rotate(camera.getRotation().invert());
////            matrix4fStack.rotateX(-camera.getPitch() * (float) (Math.PI / 180.0));
////            matrix4fStack.rotateY(camera.getYaw() * (float) (Math.PI / 180.0));
//
//            RenderPipeline renderPipeline = RenderPipelines.DEBUG_QUADS;
//            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
//            GpuTexture gpuTexture = framebuffer.getColorAttachment();
//            GpuTexture gpuTexture2 = framebuffer.getDepthAttachment();
//            GpuBuffer gpuBuffer = Mod.indexBuffer.getIndexBuffer(4);
//
//            RenderSystem.setShaderColor(0.0f, 1.0f, 0.0f, 1.0f);
//
//            try (RenderPass renderPass = RenderSystem.getDevice()
//                    .createCommandEncoder()
//                    .createRenderPass(gpuTexture, OptionalInt.empty(), gpuTexture2, OptionalDouble.empty())) {
//                renderPass.setPipeline(renderPipeline);
//                renderPass.setVertexBuffer(0, Mod.vertexBuffer);
//                renderPass.setIndexBuffer(gpuBuffer, Mod.indexBuffer.getIndexType());
//                renderPass.drawIndexed(0, 4);
//            }
//            matrix4fStack.popMatrix();
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

//    @Redirect(method = "renderCrosshair", at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"
//    ))
//    private void drawGuiTexture(DrawContext instance, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, int width, int height) {
//        RenderLayer renderLayer = (RenderLayer)renderLayers.apply(sprite);
//        Matrix4f matrix4f = instance.getMatrices().peek().getPositionMatrix();
//        try {
//            Field f = DrawContext.class.getDeclaredField("vertexConsumers"); //NoSuchFieldException
//            f.setAccessible(true);
//            VertexConsumerProvider vertexConsumers = (VertexConsumerProvider) f.get(instance); //IllegalAccessException
//            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
//            vertexConsumer.vertex(matrix4f, (float)x, (float)y, 0.0F).texture(u1, v1).color(color);
//            vertexConsumer.vertex(matrix4f, (float)x, (float)y + height, 0.0F).texture(u1, v2).color(color);
//            vertexConsumer.vertex(matrix4f, (float)x + width, (float)y + height, 0.0F).texture(u2, v2).color(color);
//            vertexConsumer.vertex(matrix4f, (float)x + width, (float)y, 0.0F).texture(u2, v1).color(color);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        //instance.drawGuiTexture(renderLayers, sprite, x, y, width, height);
//    }

    @Inject(
            method = "renderCrosshair", at = @At("RETURN")
    )
    private void crosshairPost(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            context.getMatrices().pop();
        }
    }
}
