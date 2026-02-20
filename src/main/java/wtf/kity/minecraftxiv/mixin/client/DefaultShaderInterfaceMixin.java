package wtf.kity.minecraftxiv.mixin.client;

import com.mojang.blaze3d.opengl.*;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.*;
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
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

//? <1.21 {
/*import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
@Mixin(ChunkShaderInterface.class)
*///? } else {
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;

import java.nio.ByteBuffer;
import java.util.Map;

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
    private GlUniformBool uniformDoCulling;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initPost(ShaderBindingContext context, ChunkShaderOptions options, CallbackInfo ci) {
        this.uniformPlayerPos = context.bindUniform("u_PlayerPos", GlUniformFloat3v::new);
        this.uniformEyePos = context.bindUniform("u_EyePos", GlUniformFloat3v::new);
        this.uniformCameraPos = context.bindUniform("u_CameraPos", GlUniformFloat3v::new);
        this.uniformDoCulling = context.bindUniform("u_DoCulling", GlUniformBool::new);
    }

    @Inject(method = "setupState", at = @At("TAIL"))
    public void setupStatePost(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler, CallbackInfo ci) {
        this.uniformDoCulling.set(Mod.doCulling);
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
