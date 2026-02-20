package wtf.kity.minecraftxiv.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.mod.Mod;

import java.lang.Math;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Unique
    private Matrix4f projection;
    @Unique
    private Matrix4f modelViewInv;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"
            )
    )
    private void renderLevelPre(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean bl,
            Camera camera,
            Matrix4f matrix4f,
            Matrix4f matrix4f2,
            Matrix4f matrix4f3,
            GpuBufferSlice gpuBufferSlice,
            Vector4f vector4f,
            boolean bl2,
            CallbackInfo ci
    ) {
        projection = matrix4f2;
        modelViewInv = matrix4f.invert(new Matrix4f());
    }

    @ModifyArg(
            method = "addMainPass",
            at = @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"
            )
    )
    private Runnable executes(
            Runnable runnable,
            @Local(argsOnly = true) DeltaTracker deltaTracker
    ) {
        return () -> {
            if (Mod.enabled) {
                RenderTarget renderTarget = this.minecraft.getMainRenderTarget();
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 0.0);
                Mod.doCulling = false;
                GL11.glDepthFunc(GL11.GL_GREATER);
                runnable.run();
                Mod.doCulling = true;
                GL11.glDepthFunc(GL11.GL_LESS);
                runnable.run();

                if (!Mod.moving) {
                    Vector2d res = new Vector2d(renderTarget.width, renderTarget.height);
                    GameRenderer gameRenderer = minecraft.gameRenderer;
                    Camera camera = gameRenderer.getMainCamera();
                    Entity cameraEntity = camera.entity();
                    float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);

                    double aspect = res.x / res.y;
                    Vector2d coords = new Vector2d(minecraft.mouseHandler.xpos, minecraft.mouseHandler.ypos).div(res).mul(2.0).sub(new Vector2d(1.0));
                    double fov2 = Math.toRadians(gameRenderer.getFov(camera, tickDelta, true)) / 2.0;

                    float[] depthBuf = {0};
                    double[] cursorXBuf = {0};
                    double[] cursorYBuf = {0};
                    GLFW.glfwGetCursorPos(minecraft.getWindow().handle(), cursorXBuf, cursorYBuf);
                    GL11.glReadPixels((int) cursorXBuf[0], renderTarget.height - (int) cursorYBuf[0], 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuf);
                    Vector3f projected = new Vector3f((float) cursorXBuf[0], (float) cursorYBuf[0], depthBuf[0]);
                    float depth = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, tickDelta, true))
                            .unproject(projected, new int[] {0, 0, renderTarget.width, renderTarget.height}, new Vector3f()).length();

                    coords.x *= aspect;
                    coords.y = -coords.y;
                    Vector2d offsets = coords.mul(Math.tan(fov2));
                    Vector3d forward = camera.rotation().transform(new Vector3d(0.0, 0.0, /*?>=1.21>>+'-'*/-1.0));
                    Vector3d right = camera.rotation().transform(new Vector3d(/*?<1.21>>+'-'*//*-*/1.0, 0.0, 0.0));
                    Vector3d up = camera.rotation().transform(new Vector3d(0.0, 1.0, 0.0));
                    Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
                    Vec3 rayDir = new Vec3(dir.x, dir.y, dir.z);

                    //? <1.21.11 {
                    /*Vec3 start = camera.getPosition();
                     *///? } else {
                    Vec3 start = camera.position();
                    //? }
                    Vec3 end = start.add(rayDir.scale(gameRenderer.getDepthFar()));
                    start = start.add(rayDir.scale(depth));

                    // if we're elytra flying/sprint swimming, only target blocks in front of the player so we don't get caught on algae and shit
                    if (cameraEntity instanceof Player player && player.isFallFlying() || cameraEntity.isSwimming()) {
                        System.out.println("Swimming!");
                        Vec3 eye = cameraEntity.getEyePosition(tickDelta);
                        start = start.add(rayDir.scale(
                                (start.distanceToSqr(end) + start.distanceToSqr(eye) - eye.distanceToSqr(end))
                                        / (2 * start.distanceTo(end)) + 1
                        ));
                    }

                    AABB box = cameraEntity
                            .getBoundingBox()
                            .expandTowards(rayDir.scale(gameRenderer.getDepthFar()))
                            .inflate(1.0, 1.0, 1.0);
                    HitResult hitResult = ProjectileUtil.getEntityHitResult(
                            cameraEntity,
                            start,
                            end,
                            box,
                            entity -> !entity.isSpectator() && entity.isPickable(),
                            gameRenderer.getDepthFar()
                    );
                    if (hitResult == null) {
                        hitResult = cameraEntity.level().clip(new ClipContext(
                                start,
                                end,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                cameraEntity
                        ));
                    }
                    Mod.crosshairTarget = hitResult;
                    minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, hitResult.getLocation());
                }
            } else {
                Mod.doCulling = false;
                runnable.run();
            }
            // Reset
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        };
    }
}
