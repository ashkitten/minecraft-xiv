package wtf.kity.minecraftxiv.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
    @org.jspecify.annotations.Nullable
    public ClientLevel level;

    @Shadow
    private @org.jspecify.annotations.Nullable Entity cameraEntity;

    @Shadow
    @Final
    private DeltaTracker.Timer deltaTracker;

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (this.player == null || this.level == null) {
            return;
        }
        // For some reason, KeyBinding#wasPressed doesn't work here, so I'm using KeyBinding#isPressed, which doesn't
        // seem to break anything.
        //if (ClientInit.getInstance().getKeyBinding().wasPressed() || (this.options.togglePerspectiveKey.wasPressed
        // () && mod.isEnabled())) {
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
            Minecraft.getInstance().mouseHandler.grabMouse();
        }

        if (Mod.enabled) {
            if (ClientInit.zoomInBinding.consumeClick()) {
                Mod.zoom = Math.max(Mod.zoom - 0.1f, 0.0f);
            }

            if (ClientInit.zoomOutBinding.consumeClick()) {
                Mod.zoom = Math.min(Mod.zoom + 0.1f, 2.0f);
            }

            if (Config.GSON.instance().lockOnTargeting && ClientInit.cycleTargetBinding.consumeClick()) {
                // Wrap around if we're already targeting, but we don't hit anything
                int wrapAround = Mod.lockOnTarget != null ? 1 : 0;
                do {
                    Mod.lockOnTarget = StreamSupport.stream(level.entitiesForRendering().spliterator(), true)
                            .filter(
                                    entity -> {
                                        if (entity == player) return false;
                                        if (!entity.isAttackable()) return false;
                                        if (entity.isInvisibleTo(player)) return false;
                                        if (Mod.lockOnTarget != null &&
                                                player.distanceTo(entity) <= player.distanceTo(Mod.lockOnTarget)) {
                                            return false;
                                        }

                                        // No blocks in the way
                                        return player.level().clip(new ClipContext(
                                                player.getEyePosition(deltaTracker.getGameTimeDeltaPartialTick(true)),
                                                entity.getEyePosition(),
                                                ClipContext.Block.OUTLINE,
                                                ClipContext.Fluid.NONE,
                                                player
                                        )).getType() == HitResult.Type.MISS;
                                    })
                            .min(Comparator.comparingDouble(player::distanceTo))
                            .orElse(null);
                } while (Mod.lockOnTarget == null && wrapAround-- > 0);
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

    @Inject(method = "disconnectFromWorld(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    public void disconnectPre(Component reasonText, CallbackInfo ci) {
        ClientInit.capabilities = null;
    }
}