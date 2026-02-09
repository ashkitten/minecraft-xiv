package wtf.kity.minecraftxiv.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Entity entity;

    @Unique
    private float zoom;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V")
    )
    public void setRotation(Args args) {
        if (Mod.enabled) {
            args.setAll(Mod.yaw, Mod.pitch);
        }
    }

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    )
    public void setPosition(Args args) {
        if (Mod.enabled) {
            Vec3 pos = Util.getGroundedEyePos().add(0.0, entity.getEyeHeight(Pose.STANDING), 0.0);
            args.setAll(pos.x, pos.y, pos.z);
        }
    }

    //? <1.21 {
    /*@Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D", ordinal = 0))
    public double getMaxZoom(Camera instance, double cameraDist) {
    *///? } else {
    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F", ordinal = 0))
    public float getMaxZoom(Camera instance, float cameraDist) {
    //? }
        if (Mod.enabled) {
            this.setRotation(Mod.yaw, Mod.pitch);
            return (float) cameraDist * Mod.zoom;
        } else {
            zoom = (float) instance.getMaxZoom((float) cameraDist);
        }
        return zoom;
    }
}
