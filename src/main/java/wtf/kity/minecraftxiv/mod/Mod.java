package wtf.kity.minecraftxiv.mod;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Unique;
import wtf.kity.minecraftxiv.util.Util;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class Mod {
    public static float yaw;
    public static float pitch;
    public static float zoom = 1.0f;
    public static boolean enabled = false;
    public static Perspective lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity lockOnTarget;
    public static Vec3d lockOnMouseTarget;
    public static boolean mouseMoveMode = false;
    public static GpuBuffer vertexBuffer;
    public static RenderSystem.ShapeIndexBuffer indexBuffer;

    public static void updateLockOnTarget(Camera camera, List<Entity> output, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d rayDir = Util.getMouseRay();
        // TODO: finish impl mouse-centered lock-on

        // Wrap around if we're already targeting, but we don't hit anything
        int wrapAround = lockOnTarget != null ? 1 : 0;
        do {
            lockOnTarget = output
                    .stream()
                    .filter(
                            entity -> {
                                if (entity == player) return false;
                                if (!entity.isAttackable()) return false;
                                if (entity.isInvisibleTo(player)) return false;
                                if (lockOnTarget != null &&
                                        player.distanceTo(entity) <= player.distanceTo(lockOnTarget)) {
                                    return false;
                                }

                                return player.getWorld().raycast(new BlockStateRaycastContext(
                                        camera.getPos(),
                                        entity.getEyePos(),
                                        state -> !state.isTransparent()
                                )).getType() == HitResult.Type.MISS;
                            }
                    )
                    .min(Comparator.comparingDouble(player::distanceTo))
                    .orElse(null);
        } while (lockOnTarget == null && wrapAround-- > 0);
    }

    @Unique
    public static void renderOverlay(MinecraftClient client, RenderSystem.ShapeIndexBuffer indexBuffer, GpuBuffer vertexBuffer, DrawContext context, RenderTickCounter tickCounter) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d pos = camera.getFocusedEntity().getLerpedPos(tickCounter.getTickProgress(true))
                .add(new Vec3d(0.0, camera.getFocusedEntity().getEyeHeight(camera.getFocusedEntity().getPose()), 0.0));
        Vector3f relPos = pos.subtract(camera.getPos()).toVector3f();

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.mul(context.getMatrices().peek().getPositionMatrix());
        matrix4fStack.translate(context.getScaledWindowWidth() / 2, context.getScaledWindowHeight() / 2, 0.0F);
        matrix4fStack.translate(relPos);
        matrix4fStack.rotateX((float) Math.toRadians(MathHelper.wrapDegrees(Mod.pitch)));
        matrix4fStack.rotateY(-(float) Math.toRadians(MathHelper.wrapDegrees(Mod.yaw)));

        RenderPipeline renderPipeline = RenderPipelines.DEBUG_QUADS;
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        GpuTexture gpuTexture = framebuffer.getColorAttachment();
        GpuTexture gpuTexture2 = framebuffer.getDepthAttachment();

        GpuBuffer gpuBuffer = indexBuffer.getIndexBuffer(4);

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gpuTexture, OptionalInt.empty(), gpuTexture2, OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipeline);
            RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, 0.5F);
            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.setIndexBuffer(gpuBuffer, indexBuffer.getIndexType());
            renderPass.drawIndexed(0, 4);
        }
        matrix4fStack.popMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}