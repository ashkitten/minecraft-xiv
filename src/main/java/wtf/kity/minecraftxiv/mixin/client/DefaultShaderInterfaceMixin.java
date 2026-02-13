package wtf.kity.minecraftxiv.mixin.client;

import com.mojang.blaze3d.opengl.*;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat2v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderTextureSlot;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.util.Util;

//? <1.21 {
/*import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
@Mixin(ChunkShaderInterface.class)
*///? } else {
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.OptionalDouble;

@Mixin(DefaultShaderInterface.class)
//? }

public class DefaultShaderInterfaceMixin {
    @Shadow
    @Final
    private Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    @Unique
    private GlUniformFloat3v uniformPlayerPos;
    @Unique
    private GlUniformFloat3v uniformEyePos;
    @Unique
    private GlUniformFloat3v uniformCameraPos;
    @Unique
    private GlUniformFloat2v uniformResolution;
    @Unique
    private GlUniformInt uniformDepthBuffer;
    @Unique
    private GlSampler depthSampler;
    @Unique
    private GlUniformInt uniformPositionBuffer;
    @Unique
    private GpuTexture positionTexture;
    @Unique
    private GlSampler positionSampler;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initPost(ShaderBindingContext context, ChunkShaderOptions options, CallbackInfo ci) {
        this.uniformPlayerPos = context.bindUniform("u_PlayerPos", GlUniformFloat3v::new);
        this.uniformEyePos = context.bindUniform("u_EyePos", GlUniformFloat3v::new);
        this.uniformCameraPos = context.bindUniform("u_CameraPos", GlUniformFloat3v::new);
        this.uniformResolution = context.bindUniform("u_Resolution", GlUniformFloat2v::new);
//        this.uniformDepthBuffer = context.bindUniform("u_DepthBuffer", GlUniformInt::new);
//        this.depthSampler = new GlSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, 0, OptionalDouble.empty());
        this.uniformPositionBuffer = context.bindUniform("u_PositionBuffer", GlUniformInt::new);
        this.positionSampler = new GlSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, 0, OptionalDouble.empty());
    }

    @Inject(method = "setupState", at = @At("TAIL"))
    public void setupStatePost(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler, CallbackInfo ci) {
        RenderTarget target = pass.getTarget();

        int slotOrdinal = uniformTextures.size();

        if (positionTexture != null) positionTexture.close();

        GpuDevice gpuDevice = RenderSystem.getDevice();
        GlTexture positionTexture = (GlTexture) gpuDevice.createTexture((@Nullable String) null, 15, TextureFormat.RGBA8, target.width, target.height, 1, 1);

//        if (positionTexture != 0) GlStateManager._deleteTexture(positionTexture);
//        positionTexture = GlStateManager._genTexture();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + slotOrdinal);
        GlStateManager._bindTexture(positionTexture.glId());
//        GL11.glTexImage2D(
//                GL11.GL_TEXTURE_2D,
//                0,
//                GL11.GL_RGB,
//                target.width,
//                target.height,
//                0,
//                GL11.GL_RGB,
//                GL11.GL_FLOAT,
//                0
//        );
//        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
//        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

//        GL44.glClearTexImage(positionTexture.glId(), 0, GL11.GL_RGB, GL11.GL_FLOAT, (@Nullable ByteBuffer) null);

        GL33C.glBindSampler(slotOrdinal, this.positionSampler.getId());
        this.uniformPositionBuffer.set(slotOrdinal);

        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, positionTexture.glId(), 0);

        GL20.glDrawBuffers(new int[] {
                GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_COLOR_ATTACHMENT1,
        });

        GlStateManager._glBindFramebuffer(
                GlConst.GL_FRAMEBUFFER,
                ((GlTexture) target.getColorTexture()).getFbo(
                        ((GlDevice) RenderSystem.getDevice()).directStateAccess(),
                        positionTexture
                )
        );

        this.uniformResolution.set(target.width, target.height);

//        GlTexture depth = (GlTexture) target.getDepthTexture();
//        assert depth != null;
//        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slotOrdinal);
//        GlStateManager._bindTexture(depth.glId());
//        GL33C.glBindSampler(++slotOrdinal, this.depthSampler.getId());
//        this.uniformDepthBuffer.set(slotOrdinal);
    }

    @Inject(method = "setProjectionMatrix", at = @At("TAIL"))
    public void setProjectionMatrixPost(Matrix4fc matrix, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();

        Vec3 playerPos = Util.getGroundedEyePos();
        //? <1.21.11 {
        /*Vec3 eyePos = playerPos.add(0.0, camera.getEntity().getEyeHeight(Pose.STANDING), 0.0);
        Vec3 cameraPos = camera.getPosition();
        *///? } else {
        Vec3 eyePos = playerPos.add(0.0, camera.entity().getEyeHeight(Pose.STANDING), 0.0);
        Vec3 cameraPos = camera.position();
        //? }

        this.uniformPlayerPos.set((float) playerPos.x, (float) playerPos.y, (float) playerPos.z);
        this.uniformEyePos.set((float) eyePos.x, (float) eyePos.y, (float) eyePos.z);
        this.uniformCameraPos.set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
    }
}
