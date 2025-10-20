package wtf.kity.minecraftxiv.mixin;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.mod.Mod;

import java.util.function.BiFunction;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method = "initRenderer", at = @At("TAIL"))
    private static void initRenderer(long windowHandle, int debugVerbosity, boolean sync, BiFunction<Identifier, ShaderType, String> shaderSourceGetter, boolean renderDebugLabels, CallbackInfo ci) {
        GpuBuffer vertexBuffer;
        RenderSystem.ShapeIndexBuffer indexBuffer;

        try (BufferAllocator bufferAllocator = new BufferAllocator(VertexFormats.POSITION.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            bufferBuilder.vertex(-10.0F, 0.0F, -10.0F);
            bufferBuilder.vertex(-10.0F, 0.0F, 10.0F);
            bufferBuilder.vertex(10.0F, 0.0F, -10.0F);
            bufferBuilder.vertex(10.0F, 0.0F, 10.0F);

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                Mod.vertexBuffer = RenderSystem.getQuadVertexBuffer();
                Mod.vertexBuffer = RenderSystem.getDevice()
                        .createBuffer(() -> "Targeting mode overlay", BufferType.VERTICES, BufferUsage.STATIC_WRITE, builtBuffer.getBuffer());
            }
        }
        Mod.indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
//
//        HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) -> {
//            MinecraftClient client = MinecraftClient.getInstance();
//            Window window = client.getWindow();
//            Camera camera = client.gameRenderer.getCamera();
//            Vec3d pos = camera.getFocusedEntity().getLerpedPos(tickDeltaManager.getTickProgress(true))
//                    .add(new Vec3d(0.0, camera.getFocusedEntity().getEyeHeight(camera.getFocusedEntity().getPose()), 0.0));
//            Vector3f relPos = pos.subtract(camera.getPos()).toVector3f();
//
//            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
//            matrix4fStack.pushMatrix();
//            matrix4fStack.identity();
//            matrix4fStack.mul(drawContext.getMatrices().peek().getPositionMatrix());
//            matrix4fStack.mul(RenderSystem.getProjectionMatrix());
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
//        });

    }
}
