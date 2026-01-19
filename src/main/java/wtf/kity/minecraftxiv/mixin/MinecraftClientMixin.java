package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
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

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Final
    public GameOptions options;

    @Shadow
    @org.jspecify.annotations.Nullable
    public ClientWorld world;

    @Shadow
    private @org.jspecify.annotations.Nullable Entity cameraEntity;

    @Shadow
    @Final
    private RenderTickCounter.Dynamic renderTickCounter;

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (this.player == null || this.world == null) {
            return;
        }
        // For some reason, KeyBinding#wasPressed doesn't work here, so I'm using KeyBinding#isPressed, which doesn't
        // seem to break anything.
        //if (ClientInit.getInstance().getKeyBinding().wasPressed() || (this.options.togglePerspectiveKey.wasPressed
        // () && mod.isEnabled())) {
        if (ClientInit.toggleBinding.wasPressed() || this.options.togglePerspectiveKey.isPressed() && Mod.enabled) {
            if (Mod.enabled) {
                options.setPerspective(Mod.lastPerspective);
                Util.debug("Disabled Minecraft XIV");
            } else {
                Mod.lastPerspective = this.options.getPerspective();
                this.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                if (Mod.lastPerspective == Perspective.THIRD_PERSON_FRONT) {
                    Mod.yaw = ((180 + this.player.getYaw() + 180) % 360) - 180;
                    Mod.pitch = -this.player.getPitch();
                } else {
                    Mod.yaw = this.player.getYaw();
                    Mod.pitch = this.player.getPitch();
                }
                Util.debug("Enabled Minecraft XIV");
            }
            Mod.enabled = !Mod.enabled;

            // Re-lock the cursor so it correctly changes state
            MinecraftClient.getInstance().mouse.lockCursor();
        }

        if (Mod.enabled) {
            if (ClientInit.zoomInBinding.wasPressed()) {
                Mod.zoom = Math.max(Mod.zoom - 0.1f, 0.0f);
            }

            if (ClientInit.zoomOutBinding.wasPressed()) {
                Mod.zoom = Math.min(Mod.zoom + 0.1f, 2.0f);
            }

            if (Config.GSON.instance().lockOnTargeting && ClientInit.cycleTargetBinding.wasPressed()) {
                // Wrap around if we're already targeting, but we don't hit anything
                int wrapAround = Mod.lockOnTarget != null ? 1 : 0;
                do {
                    Mod.lockOnTarget = StreamSupport.stream(world.getEntities().spliterator(), true)
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
                                        return player.getEntityWorld().raycast(new RaycastContext(
                                                player.getCameraPosVec(renderTickCounter.getTickProgress(true)),
                                                entity.getEyePos(),
                                                RaycastContext.ShapeType.OUTLINE,
                                                RaycastContext.FluidHandling.NONE,
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

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    public void hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == Mod.lockOnTarget) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    public void disconnectPre(Text reasonText, CallbackInfo ci) {
        ClientInit.capabilities = null;
    }
}