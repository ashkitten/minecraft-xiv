package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @SuppressWarnings("unused")
    @Shadow
    private float yRot;
    @SuppressWarnings("unused")
    @Shadow
    private float xRot;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0)
    )
    public void a(Args args) {
        if (Mod.enabled) {
            args.setAll(Mod.yaw, Mod.pitch);
        }
    }

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F", ordinal = 0)
    )
    public void b(Args args) {
        if (Mod.enabled) {
            this.setRotation(Mod.yaw, Mod.pitch);
            args.set(0, (float) args.get(0) * Mod.zoom);
        }
    }
}
