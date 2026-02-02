package wtf.kity.minecraftxiv.mixin.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    public Options options;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    private @Nullable Entity cameraEntity;

    @Unique
    private double lastX;
    @Unique
    private double lastY;

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (this.player == null || this.level == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        MouseHandler mouse = client.mouseHandler;
        Window window = client.getWindow();

        if (ClientInit.toggleBinding.consumeClick() || this.options.keyTogglePerspective.isDown() && Mod.enabled) {
            if (Mod.enabled) {
                options.setCameraType(Mod.lastPerspective);
                Util.debug("Disabled Minecraft XIV");
            } else {
                Mod.lastPerspective = this.options.getCameraType();
                this.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                if (Mod.lastPerspective == CameraType.THIRD_PERSON_FRONT) {
                    Mod.yaw = ((180 + this.player.getYRot() + 180) % 360) - 180;
                    Mod.pitch = -this.player.getXRot();
                } else {
                    Mod.yaw = this.player.getYRot();
                    Mod.pitch = this.player.getXRot();
                }
                Util.debug("Enabled Minecraft XIV");
            }
            Mod.enabled = !Mod.enabled;

            // Re-lock the cursor so it correctly changes state
            client.mouseHandler.grabMouse();
        }

        if (Mod.enabled) {
            if (ClientInit.zoomInBinding.consumeClick()) {
                Mod.zoom = Math.max(Mod.zoom - 0.1f, 0.0f);
            }

            if (ClientInit.zoomOutBinding.consumeClick()) {
                Mod.zoom = Math.min(Mod.zoom + 0.1f, 2.0f);
            }

            if (Config.GSON.instance().lockOnTargeting && ClientInit.cycleTargetBinding.consumeClick() && cameraEntity != null) {
                if (Mod.crosshairTarget instanceof EntityHitResult entity) {
                    Mod.lockOnTarget = entity.getEntity();
                } else if (Mod.crosshairTarget instanceof BlockHitResult block) {
                    Mod.lockOnTarget = StreamSupport.stream(level.entitiesForRendering().spliterator(), true)
                            .filter(entity -> entity != player
                                    && entity.isAttackable()
                                    && !entity.isInvisibleTo(player)
                                    && entity.level().clip(new ClipContext(
                                    //? <26 {
                                    /*client.gameRenderer.getMainCamera().getPosition(),
                                     *///? } else {
                                    client.gameRenderer.getMainCamera().position(),
                                    //? }
                                    entity.position(),
                                    ClipContext.Block.OUTLINE,
                                    ClipContext.Fluid.NONE,
                                    cameraEntity
                            )).getType() != HitResult.Type.BLOCK)
                            .min(Comparator.comparingDouble(block::distanceTo))
                            .orElse(null);
                }
            }

            if (ClientInit.moveCameraBinding.isDown()) {
                if (!Mod.moving) {
                    Mod.moving = true;
                    lastX = mouse.xpos();
                    lastY = mouse.ypos();
                }
            } else if (Mod.moving) {
                Mod.moving = false;
                //? <26 {
                /*GLFW.glfwSetCursorPos(window.getWindow(), lastX, lastY);
                 *///? } else {
                GLFW.glfwSetCursorPos(window.handle(), lastX, lastY);
                //? }
                mouse.xpos = lastX;
                mouse.ypos = lastY;
            }
        }

        if (Mod.lockOnTarget != null && !Mod.lockOnTarget.isAlive()) {
            Mod.lockOnTarget = null;
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    public void hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == Mod.lockOnTarget) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    //? <26 {
    /*@Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    *///? } else {
    @Inject(method = "disconnectFromWorld(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    //? }
    public void disconnectFromWorld(CallbackInfo ci) {
        ClientInit.setCapabilities(null);
    }
}