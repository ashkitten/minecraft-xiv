package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Shadow
    private boolean mouseGrabbed;
    @Shadow
    @Final
    private Minecraft minecraft;
    @SuppressWarnings("unused")
    @Shadow
    private boolean ignoreFirstMove;
    @Shadow
    private double xpos;
    @Shadow
    private double ypos;

    @Unique
    @Nullable
    private Double lastX;
    @Unique
    @Nullable
    private Double lastY;

    /**
     * It doesn't make sense to "lock" the cursor of an absolute pointing device.
     *
     * @author quaternary
     */
    @Overwrite
    public void grabMouse() {
        //btw this is the ol "copy-paste overwrite"
        // TODO make it a good mixin (although i'm not really sure what for)

        if (minecraft.isWindowActive()) {
            if (!mouseGrabbed) {
                if (InputQuirks.RESTORE_KEY_STATE_AFTER_MOUSE_GRAB) {
                    KeyMapping.setAll();
                }

                mouseGrabbed = true;

                if (Mod.enabled) {
                    // Merely hide the cursor instead of "disabling" it
                    InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_HIDDEN, xpos, ypos);
                } else {
                    InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_DISABLED, xpos, ypos);
                    xpos = minecraft.getWindow().getScreenWidth() / 2.0;
                    ypos = minecraft.getWindow().getScreenHeight() / 2.0;
                }                    minecraft.setScreen(null);
                minecraft.missTime = 10000;
                ignoreFirstMove = true;
            }
        }
    }

    /**
     * It doesn't make sense to "unlock" the cursor of an absolute pointing device.
     *
     * @author quaternary
     */
    @Overwrite
    public void releaseMouse() {
        if (mouseGrabbed) {
            mouseGrabbed = false;
            if (!Mod.enabled) {
                xpos = minecraft.getWindow().getScreenWidth() / 2.0;
                ypos = minecraft.getWindow().getScreenHeight() / 2.0;
            }
            InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_NORMAL, xpos, ypos);
        }
    }

    @Inject(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onMouse(DD)V")
    )
    private void updateMouseA(
            double timeDelta, CallbackInfo ci, @Local(ordinal = 1) double i, @Local(ordinal = 2) double j
    ) {
        GameRenderer renderer = minecraft.gameRenderer;
        Window window = minecraft.getWindow();
        MouseHandler mouse = minecraft.mouseHandler;
        Camera camera = renderer.getMainCamera();
        float tickDelta = camera.getPartialTickTime();
        Entity cameraEntity = minecraft.getCameraEntity();

        if (Mod.enabled && cameraEntity != null && minecraft.player != null) {
            if (ClientInit.moveCameraBinding.isDown()) {
                if (lastX == null || lastY == null) {
                    lastX = xpos;
                    lastY = ypos;
                    xpos = window.getWidth() / 2.0;
                    ypos = window.getHeight() / 2.0;
                    InputConstants.grabOrReleaseMouse(window, InputConstants.CURSOR_DISABLED, xpos, ypos);
                }
                float yaw1 = (float) (Mod.yaw + i / 8.0D);
                float pitch1 = (float) (Mod.pitch + j / 8.0D);
                Mod.yaw = yaw1;
                Mod.pitch = pitch1;
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
                if (lastX != null && lastY != null) {
                    InputConstants.grabOrReleaseMouse(
                            minecraft.getWindow(),
                            GLFW.GLFW_CURSOR_HIDDEN,
                            lastX,
                            lastY
                    );
                    xpos = lastX;
                    ypos = lastY;
                    lastX = null;
                    lastY = null;
                }

                Vector2d res = new Vector2d(window.getWidth(), window.getHeight());
                double aspect = res.x / res.y;
                Vector2d coords = new Vector2d(mouse.xpos(), mouse.ypos()).div(res).mul(2.0).sub(new Vector2d(1.0));
                double fov2 =
                        Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, tickDelta, true)) / 2.0;
                coords.x *= aspect;
                coords.y = -coords.y;
                Vector2d offsets = coords.mul(Math.tan(fov2));
                Vector3d forward = camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0));
                Vector3d right = camera.rotation().transform(new Vector3d(1.0, 0.0, 0.0));
                Vector3d up = camera.rotation().transform(new Vector3d(0.0, 1.0, 0.0));
                Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
                Vec3 rayDir = new Vec3(dir.x, dir.y, dir.z);

                Vec3 start = camera.position();
                Vec3 end = start.add(rayDir.scale(renderer.getDepthFar()));

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
    private void updateMouseB(CallbackInfo info) {
        if (Mod.enabled) {
            info.cancel();
        }
    }

    @Redirect(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I")
    )
    private int scrollCycling(double amount, int selectedIndex, int total) {
        if (Mod.enabled && Config.GSON.instance().scrollWheelZoom) {
            Mod.zoom = Math.max(0.0f, Mod.zoom - (float) amount * 0.2f);
            return selectedIndex;
        }
        return ScrollWheelHandler.getNextScrollWheelSelection(amount, selectedIndex, total);
    }
}
