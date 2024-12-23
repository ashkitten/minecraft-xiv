package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.Scroller;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow
    private boolean cursorLocked;
    @Shadow
    @Final
    private MinecraftClient client;
    @SuppressWarnings("unused")
    @Shadow
    private boolean hasResolutionChanged;
    @Shadow
    private double x;
    @Shadow
    private double y;

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
    public void lockCursor() {
        //btw this is the ol "copy-paste overwrite"
        // TODO make it a good mixin (although i'm not really sure what for)

        if (client.isWindowFocused()) {
            if (!cursorLocked) {
                if (!MinecraftClient.IS_SYSTEM_MAC) {
                    KeyBinding.updatePressedStates();
                }

                cursorLocked = true;

                if (Mod.enabled) {
                    // Merely hide the cursor instead of "disabling" it
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_HIDDEN, x, y);
                } else {
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_DISABLED, x, y);

                    x = client.getWindow().getWidth() / 2.0;
                    y = client.getWindow().getHeight() / 2.0;
                }

                client.setScreen(null);

                // This has protected access and i don't wanna AW it lmao hope this works
                //client.attackCooldown = 10000;

                hasResolutionChanged = true;
            }
        }
    }

    /**
     * It doesn't make sense to "unlock" the cursor of an absolute pointing device.
     *
     * @author quaternary
     */
    @Overwrite
    public void unlockCursor() {
        if (cursorLocked) {
            cursorLocked = false;
            if (!Mod.enabled) {
                x = client.getWindow().getWidth() / 2.0;
                y = client.getWindow().getHeight() / 2.0;
            }
            InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, x, y);
        }
    }

    @Inject(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "net/minecraft/client/tutorial/TutorialManager.onUpdateMouse(DD)V")
    )
    private void updateMouseA(
            double timeDelta, CallbackInfo ci, @Local(ordinal = 1) double i, @Local(ordinal = 2) double j, @Local int k
    ) {
        GameRenderer renderer = client.gameRenderer;
        Window window = client.getWindow();
        Mouse mouse = client.mouse;
        Camera camera = renderer.getCamera();
        float tickDelta = camera.getLastTickDelta();
        Entity cameraEntity = client.cameraEntity;

        if (Mod.enabled && cameraEntity != null && client.player != null) {
            if (ClientInit.moveCameraBinding.isPressed()) {
                if (lastX == null || lastY == null) {
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), InputUtil.GLFW_CURSOR_DISABLED,
                            window.getFramebufferWidth() / 2.0, window.getFramebufferHeight() / 2.0
                    );
                    lastX = x;
                    lastY = y;
                }
                float yaw1 = (float) (Mod.yaw + i / 8.0D);
                float pitch1 = (float) (Mod.pitch + j * k / 8.0D);
                Mod.yaw = yaw1;
                Mod.pitch = pitch1;
                if (Math.abs(Mod.pitch) > 90.0F) {
                    float yaw = Mod.yaw;
                    float pitch = (Mod.pitch > 0.0F) ? 90.0F : -90.0F;
                    Mod.yaw = yaw;
                    Mod.pitch = pitch;
                }

                this.client.player.setYaw(Mod.yaw);
                this.client.player.setPitch(Mod.pitch);

                Mod.crosshairTarget = null;
            } else {
                if (lastX != null && lastY != null) {
                    InputUtil.setCursorParameters(
                            client.getWindow().getHandle(),
                            GLFW.GLFW_CURSOR_HIDDEN,
                            lastX,
                            lastY
                    );
                    x = lastX;
                    y = lastY;
                    lastX = null;
                    lastY = null;
                }

                Vector2d res = new Vector2d(window.getFramebufferWidth(), window.getFramebufferHeight());
                double aspect = res.x / res.y;
                Vector2d coords = new Vector2d(mouse.getX(), mouse.getY()).div(res).mul(2.0).sub(new Vector2d(1.0));
                double fov2 =
                        Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, tickDelta, true)) / 2.0;
                coords.x *= aspect;
                coords.y = -coords.y;
                Vector2d offsets = coords.mul(Math.tan(fov2));
                Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
                Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
                Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
                Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
                Vec3d rayDir = new Vec3d(dir.x, dir.y, dir.z);

                Vec3d start = camera.getPos();
                Vec3d end = start.add(rayDir.multiply(renderer.getFarPlaneDistance()));

                Box box = cameraEntity
                        .getBoundingBox()
                        .stretch(rayDir.multiply(renderer.getFarPlaneDistance()))
                        .expand(1.0, 1.0, 1.0);
                HitResult hitResult = ProjectileUtil.raycast(
                        cameraEntity,
                        start,
                        end,
                        box,
                        entity -> !entity.isSpectator() && entity.canHit(),
                        renderer.getFarPlaneDistance()
                );
                if (hitResult == null) {
                    hitResult = cameraEntity.getWorld().raycast(new RaycastContext(
                            start,
                            end,
                            RaycastContext.ShapeType.OUTLINE,
                            RaycastContext.FluidHandling.NONE,
                            cameraEntity
                    ));
                }
                Mod.crosshairTarget = hitResult;
                client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, hitResult.getPos());
            }
        }
    }

    @Inject(
            method = { "updateMouse" }, at = {
            @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/network/ClientPlayerEntity.changeLookDirection(DD)V"
            )
    }, cancellable = true
    )
    private void updateMouseB(CallbackInfo info) {
        if (Mod.enabled) {
            info.cancel();
        }
    }

    @Redirect(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Scroller;scrollCycling(DII)I")
    )
    private int scrollCycling(double amount, int selectedIndex, int total) {
        if (Mod.enabled && Config.GSON.instance().scrollWheelZoom) {
            Mod.zoom = Math.max(0.0f, Mod.zoom - (float) amount * 0.2f);
            return selectedIndex;
        }
        return Scroller.scrollCycling(amount, selectedIndex, total);
    }
}
