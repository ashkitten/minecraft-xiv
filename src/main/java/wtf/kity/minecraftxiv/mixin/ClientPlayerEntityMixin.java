package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow
    private static HitResult filterHitResult(HitResult hitResult, Vec3 cameraPos, double range) {
        return null;
    }


    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("HEAD"), cancellable = true)
    private static void getCrosshairTarget(
            Entity camera,
            double blockInteractionRange,
            double entityInteractionRange,
            float tickProgress,
            CallbackInfoReturnable<HitResult> cir
    ) {
        if (Config.GSON.instance().lockOnTargeting && Mod.lockOnTarget != null) {
            camera.lookAt(EntityAnchorArgument.Anchor.EYES, Mod.lockOnTarget.getEyePosition());
        } else if (Config.GSON.instance().targetFromCamera && ClientInit.getCapabilities().targetFromCamera()) {
            HitResult target = Mod.crosshairTarget;
            if (target == null) return;
            if (!Config.GSON.instance().unlimitedReach) {
                target = filterHitResult(
                        target,
                        camera.getEyePosition(tickProgress),
                        blockInteractionRange
                );
            }
            cir.setReturnValue(target);
            cir.cancel();
        }
    }
}
