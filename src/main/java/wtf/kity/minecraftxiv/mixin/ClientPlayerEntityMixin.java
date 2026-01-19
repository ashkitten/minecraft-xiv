package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow
    private static HitResult checkCrosshairTargetRange(HitResult hitResult, Vec3d cameraPos, double range) {
        return null;
    }


    @Inject(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At("HEAD"), cancellable = true)
    private static void getCrosshairTarget(
            Entity camera,
            double blockInteractionRange,
            double entityInteractionRange,
            float tickProgress,
            CallbackInfoReturnable<HitResult> cir
    ) {
        if (Config.GSON.instance().lockOnTargeting && Mod.lockOnTarget != null) {
            camera.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, Mod.lockOnTarget.getEyePos());
        } else if (Config.GSON.instance().targetFromCamera && ClientInit.getCapabilities().targetFromCamera()) {
            HitResult target = Mod.crosshairTarget;
            if (target == null) return;
            if (!Config.GSON.instance().unlimitedReach) {
                target = checkCrosshairTargetRange(
                        target,
                        camera.getCameraPosVec(tickProgress),
                        blockInteractionRange
                );
            }
            cir.setReturnValue(target);
            cir.cancel();
        }
    }
}
