package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState> {
    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;"
                    + "Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render"
                            + "(Lnet/minecraft/client/util/math/MatrixStack;"
                            + "Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    public void render(
            EntityModel<? super S> instance,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            @Local(argsOnly = true) S livingEntityRenderState,
            @Local(argsOnly = true) VertexConsumerProvider vertexConsumerProvider
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        ClientPlayerEntity player = client.player;
        Window window = client.getWindow();

        if (livingEntityRenderState instanceof PlayerEntityRenderState playerEntityRenderState
                && player != null
                && playerEntityRenderState.id == player.getId()
                && Mod.enabled) {
            if (camera.getPos().distanceTo(player.getEyePos()) < 1.0 || camera.getPos().distanceTo(player.getPos()) < 1.0) {
                vertices = vertexConsumerProvider.getBuffer(RenderLayer.getItemEntityTranslucentCull(player
                        .getSkinTextures()
                        .texture()));

                double minDist = Math.min(camera.getPos().distanceTo(player.getEyePos()), camera.getPos().distanceTo(player.getPos()));
                // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
                int alpha = Math.clamp((int) (minDist * 255.0), 0x26, 0xff);
                color = 0xFFFFFF | alpha << 24;
            }

            matrices.push();
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            Vector3f[] verts = {
                    new Vector3f(-100.0f, 0.0f, -100.0f),
                    new Vector3f(100.0f, 0.0f, -100.0f),
                    new Vector3f(-100.0f, 0.0f, 100.0f),
                    new Vector3f(100.0f, 0.0f, 100.0f),
            };

            Vector3f vector3f = new Vector3f();
            Vector3f vector3f2 = matrices.peek().transformNormal(new Vector3f(0.0f, 1.0f, 0.0f), vector3f);
            float f = vector3f2.x();
            float g = vector3f2.y();
            float h = vector3f2.z();

            for (Vector3f vertex : verts) {
                float i = vertex.x / 16.0F;
                float j = vertex.y / 16.0F;
                float k = vertex.z / 16.0F;
                Vector3f vector3f3 = matrix4f.transformPosition(i, j, k, vector3f);
                vertices.vertex(vector3f3.x(), vector3f3.y(), vector3f3.z(), color, 0.0f, 0.0f, overlay, light, f, g, h);
            }
            matrices.pop();

//            Vec3d pos = camera.getFocusedEntity().getLerpedPos(client.getRenderTickCounter().getTickProgress(true))
//                    .add(new Vec3d(0.0, camera.getFocusedEntity().getEyeHeight(camera.getFocusedEntity().getPose()), 0.0));
//            Vector3f relPos = pos.subtract(camera.getPos()).toVector3f();
//
//            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
//            matrix4fStack.pushMatrix();
//            matrix4fStack.mul(matrices.peek().getPositionMatrix());
//            matrix4fStack.translate(window.getScaledWidth() / 2.0f, window.getScaledHeight() / 2.0f, 0.0f);
//            matrix4fStack.translate(relPos);
//            matrix4fStack.rotateX((float) Math.toRadians(MathHelper.wrapDegrees(Mod.pitch)));
//            matrix4fStack.rotateY(-(float) Math.toRadians(MathHelper.wrapDegrees(Mod.yaw)));
//
//            RenderPipeline renderPipeline = RenderPipelines.DEBUG_QUADS;
//            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
//            GpuTexture gpuTexture = framebuffer.getColorAttachment();
//            GpuTexture gpuTexture2 = framebuffer.getDepthAttachment();
//
//            GpuBuffer gpuBuffer = Mod.indexBuffer.getIndexBuffer(4);
//
//            try (RenderPass renderPass = RenderSystem.getDevice()
//                    .createCommandEncoder()
//                    .createRenderPass(gpuTexture, OptionalInt.empty(), gpuTexture2, OptionalDouble.empty())) {
//                renderPass.setPipeline(renderPipeline);
//                RenderSystem.setShaderColor(0.0f, 1.0f, 0.0f, 0.5f);
//                renderPass.setVertexBuffer(0, Mod.vertexBuffer);
//                renderPass.setIndexBuffer(gpuBuffer, Mod.indexBuffer.getIndexType());
//                renderPass.drawIndexed(0, 4);
//            }
//            matrix4fStack.popMatrix();
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//
        }

        instance.render(matrices, vertices, light, overlay, color);
    }
}
