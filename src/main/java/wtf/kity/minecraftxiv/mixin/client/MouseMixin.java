package wtf.kity.minecraftxiv.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

//? <26
//import net.minecraft.world.entity.player.Inventory;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Shadow
    private boolean mouseGrabbed;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private double xpos;
    @Shadow
    private double ypos;

    @WrapMethod(method = "onMove")
    private void onMove(long handle, double xpos, double ypos, Operation<Void> original) {
        if (!Mod.enabled || !mouseGrabbed || Mod.moving) {
            original.call(handle, xpos, ypos);
            return;
        }

        Vector2d res = new Vector2d(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        double aspect = res.x / res.y;

        int margin = 0;
        double sens = 1 / 16.0;

        if (xpos < margin) {
            Mod.yaw += (float) ((xpos - margin) * sens * aspect);
            xpos = margin;
        } else if (xpos > res.x - margin) {
            Mod.yaw += (float) ((xpos - (res.x - margin)) * sens * aspect);
            xpos = res.x - margin;
        }

        if (ypos < margin) {
            Mod.pitch += (float) ((ypos - margin) * sens);
            ypos = margin;
        } else if (ypos > res.y - margin) {
            Mod.pitch += (float) ((ypos - (res.y - margin)) * sens);
            ypos = res.y - margin;
        }

        if (Math.abs(Mod.pitch) > 90.0F) {
            Mod.yaw += (float) ((Math.abs(Mod.pitch) - 90.0F) * Math.signum(Mod.pitch) / (xpos / res.x * 2 - 1));
            Mod.pitch = (Mod.pitch > 0.0F) ? 90.0F : -90.0F;
        }

        GLFW.glfwSetCursorPos(handle, xpos, ypos);

        original.call(handle, xpos, ypos);
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onMouse(DD)V")
    )
    private void onMouse(Tutorial instance, double x, double y) {
        //? <1.21 {
        /*float tickDelta = minecraft.getDeltaFrameTime();
        *///? } else {
        float tickDelta = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        //? }

        GameRenderer renderer = minecraft.gameRenderer;
        Window window = minecraft.getWindow();
        Camera camera = renderer.getMainCamera();
        Entity cameraEntity = minecraft.getCameraEntity();

        if (Mod.enabled && cameraEntity != null && minecraft.player instanceof LocalPlayer) {
            if (Mod.moving) {
                Mod.yaw += (float) (x / 8.0D);
                Mod.pitch += (float) (y / 8.0D);
                if (Math.abs(Mod.pitch) > 90.0F) {
                    float yaw = Mod.yaw;
                    float pitch = (Mod.pitch > 0.0F) ? 90.0F : -90.0F;
                    Mod.yaw = yaw;
                    Mod.pitch = pitch;
                }

                this.minecraft.player.setYRot(Mod.yaw);
                this.minecraft.player.setXRot(Mod.pitch);

                Mod.crosshairTarget = null;
            } else {
                Vector2d res = new Vector2d(window.getWidth(), window.getHeight());
                double aspect = res.x / res.y;
                Vector2d coords = new Vector2d(xpos, ypos).div(res).mul(2.0).sub(new Vector2d(1.0));
                double fov2 = Math.toRadians(renderer.getFov(camera, (float) tickDelta, true)) / 2.0;

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
                Vec3 end = start.add(rayDir.scale(renderer.getDepthFar()));

                // if we're elytra flying/sprint swimming, only target blocks in front of the player so we don't get caught on algae and shit
                if (minecraft.player.isFallFlying() || minecraft.player.isSwimming()) {
                    System.out.println("Swimming!");
                    Vec3 eye = cameraEntity.getEyePosition((float) tickDelta);
                    start = start.add(rayDir.scale(
                            (start.distanceToSqr(end) + start.distanceToSqr(eye) - eye.distanceToSqr(end))
                                    / (2 * start.distanceTo(end)) + 1
                    ));
                }

                AABB box = cameraEntity
                        .getBoundingBox()
                        .expandTowards(rayDir.scale(renderer.getDepthFar()))
                        .inflate(1.0, 1.0, 1.0);
                HitResult hitResult = ProjectileUtil.getEntityHitResult(
                        cameraEntity,
                        start,
                        end,
                        box,
                        entity -> !entity.isSpectator() && entity.isPickable(),
                        renderer.getDepthFar()
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
        }
    }

    @Inject(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"),
            cancellable = true
    )
    private void turn(CallbackInfo info) {
        if (Mod.enabled) {
            info.cancel();
        }
    }

    @Redirect(
            method = "onScroll",
    //? <1.21.11 {
            /*at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V")
    )
    private void scrollCycling(Inventory instance, double amount) {
    *///? } else {
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I")
        )
    private int scrollCycling(double amount, int selectedIndex, int total) {
    //? }
        if (Mod.enabled && Config.GSON.instance().scrollWheelZoom && !Util.hotbarHovered()) {
            Mod.zoom = Math.max(0.0f, Mod.zoom - (float) amount * 0.2f);

    //? <1.21.11 {
            /*return;
        }
        instance.swapPaint(amount);
    *///? } else {
            return selectedIndex;
        }
        return ScrollWheelHandler.getNextScrollWheelSelection(amount, selectedIndex, total);
    //? }
    }

    @Redirect(
            //? <1.21.11 {
            /*method = "onPress",
            *///? } else {
            method = "onButton",
            //? }
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V")
    )
    private void beforeSetKeyMapping(InputConstants.Key key, boolean pressed) {
        // it's always okay to send keyup events
        if (!Mod.enabled || !Util.hotbarHovered() || !pressed) {
            KeyMapping.set(key, pressed);
        }
    }

    @Redirect(
            //? <1.21.11 {
            /*method = "onPress",
            *///? } else {
            method = "onButton",
            //? }
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V")
    )
    private void beforeClickKeyMapping(InputConstants.Key key) {
        if (!Mod.enabled || !Util.hotbarHovered()) {
            KeyMapping.click(key);
        }
    }
}
