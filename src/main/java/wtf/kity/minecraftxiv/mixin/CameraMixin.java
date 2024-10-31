package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @SuppressWarnings("unused")
    @Shadow
    private float yaw;
    @SuppressWarnings("unused")
    @Shadow
    private float pitch;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract float clipToSpace(float f);

    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0)
    )
    public void a(Args args) {
        if (Mod.enabled) {
            args.setAll(Mod.yaw, Mod.pitch);
        }
    }

    @Redirect(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F", ordinal = 0)
    )
    public float b(Camera instance, float f) {
        if (Mod.enabled) {
            this.setRotation(Mod.yaw, Mod.pitch);
            return this.clipToSpace(f * Mod.zoom);
        } else {
            return this.clipToSpace(f);
        }
    }
}
