package wtf.kity.minecraftxiv.mixin.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.gl.device.DrawCommandList;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultChunkRenderer.class)
public class DefaultChunkRendererMixin {
    @Unique
    private static PerspectiveProjectionMatrixBuffer perspectiveProjectionMatrixBuffer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initPost(RenderDevice device, ChunkVertexType vertexType, CallbackInfo ci) {
        perspectiveProjectionMatrixBuffer = new PerspectiveProjectionMatrixBuffer("XIV projection");
    }

    @Redirect(
            method = "executeDrawBatch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/gl/device/DrawCommandList;multiDrawElementsBaseVertex(Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlIndexType;)V"
            )
    )
    private static void multiDrawElementsBaseVertex(
            DrawCommandList instance,
            MultiDrawBatch multiDrawBatch, GlIndexType glIndexType
    ) {
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        instance.multiDrawElementsBaseVertex(multiDrawBatch, glIndexType);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }
}
