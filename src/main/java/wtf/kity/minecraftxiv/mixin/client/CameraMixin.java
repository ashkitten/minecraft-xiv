package wtf.kity.minecraftxiv.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Unique
    private float zoom;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @ModifyArgs(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0)
    )
    public void setRotation(Args args) {
        if (Mod.enabled) {
            args.setAll(Mod.yaw, Mod.pitch);
        }
    }

    //? <26 {
    /*@Redirect(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D", ordinal = 0)
    )
    public double getMaxZoom(Camera instance, double cameraDist) {
        Vec3 cameraPos = instance.getPosition();
        Entity cameraEntity = instance.getEntity();
    *///? } else {
    @Redirect(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F", ordinal = 0)
    )
    public float getMaxZoom(Camera instance, float cameraDist) {
        Vec3 cameraPos = instance.position();
        Entity cameraEntity = instance.entity();
    //? }
        if (Mod.enabled) {
            this.setRotation(Mod.yaw, Mod.pitch);
            Vector3f offset = new Vector3f(0, 0, zoom).rotate(instance.rotation());
            Vec3 pos = new Vec3(cameraPos.x + offset.x, cameraPos.y + offset.y, cameraPos.z + offset.z);
            if (zoom != cameraDist * Mod.zoom || !cameraEntity.level().isEmptyBlock(BlockPos.containing(pos))) {
                zoom = (float) cameraDist * Mod.zoom;
                offset = new Vector3f(0, 0, zoom).rotate(instance.rotation());
                pos = new Vec3(cameraPos.x + offset.x, cameraPos.y + offset.y, cameraPos.z + offset.z);
                if (!cameraEntity.level().isEmptyBlock(BlockPos.containing(pos))) {
                    zoom = (float) instance.getMaxZoom((float) cameraDist * Mod.zoom);
                }
            }
        } else {
            zoom = (float) instance.getMaxZoom((float) cameraDist);
        }
        return zoom;
    }
}
