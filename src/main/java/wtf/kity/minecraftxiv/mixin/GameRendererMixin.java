package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    public abstract Camera getCamera();

    @Inject(method = "findCrosshairTarget", at = @At("HEAD"), cancellable = true)
    public void findCrosshairTarget(
            Entity camera,
            double blockInteractionRange,
            double entityInteractionRange,
            float tickDelta,
            CallbackInfoReturnable<HitResult> cir
    ) {
        if (Config.GSON.instance().lockOnTargeting && Mod.lockOnTarget != null) {
            Config.ProjectileTargeting projectileTargeting = Config.GSON.instance().projectileTargeting;
            if (projectileTargeting != Config.ProjectileTargeting.NONE
                    && camera.isControlledByPlayer() && !client.isPaused()) {
                PlayerEntity player = (PlayerEntity) camera;
                Mod.lockOnMouseTarget = Mod.lockOnTarget
                        .getLerpedPos(tickProgress)
                        .add(new Vec3d(0.0, Mod.lockOnTarget.getEyeHeight(Mod.lockOnTarget.getPose()), 0.0));
                if (projectileTargeting != Config.ProjectileTargeting.LEGACY && Util.holdingProjectile(player)) {
                    Vec3d cameraPos = getCamera().getPos();
                    Vec3d ray = Util.getMouseRay();
                    Vector3d cameraForward = getCamera().getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
                    Vec3d normal = new Vec3d(cameraForward.x, cameraForward.y, cameraForward.z).normalize();
                    Vec3d intersection = Util.lineIntersection(Mod.lockOnMouseTarget, normal, cameraPos, ray);
                    if (projectileTargeting == Config.ProjectileTargeting.CAMERA_PLANE) {
                        Mod.lockOnMouseTarget = intersection;
                    } else if (projectileTargeting == Config.ProjectileTargeting.VERTICAL_PLANE) {
                        Mod.lockOnMouseTarget = new Vec3d(Mod.lockOnMouseTarget.x, intersection.y, Mod.lockOnMouseTarget.z);
                    }
                }
                player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, Mod.lockOnMouseTarget);
            }
        } else if (Config.GSON.instance().targetFromCamera && ClientInit.getCapabilities().targetFromCamera()) {
            HitResult target = Mod.crosshairTarget;
            if (target == null) return;
            if (!Config.GSON.instance().unlimitedReach) {
                target = ((GameRendererAccessor) MinecraftClient.getInstance().gameRenderer).callEnsureTargetInRange(
                        target,
                        camera.getCameraPosVec(tickDelta),
                        blockInteractionRange
                );
            }
            cir.setReturnValue(target);
            cir.cancel();
        }
    }
}
