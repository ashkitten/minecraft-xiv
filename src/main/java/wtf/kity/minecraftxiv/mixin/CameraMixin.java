package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
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

    @Redirect(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F", ordinal = 0)
    )
    public float getMaxZoom(Camera instance, float cameraDist) {
        if (Mod.enabled) {
            this.setRotation(Mod.yaw, Mod.pitch);
            Vector3f offset = new Vector3f(0, 0, zoom).rotate(instance.rotation());
            Vec3 pos = new Vec3(instance.position().x + offset.x, instance.position().y + offset.y, instance.position().z + offset.z);
            if (zoom != cameraDist * Mod.zoom || !instance.entity().level().isEmptyBlock(BlockPos.containing(pos))) {
                zoom = cameraDist * Mod.zoom;
                offset = new Vector3f(0, 0, zoom).rotate(instance.rotation());
                pos = new Vec3(instance.position().x + offset.x, instance.position().y + offset.y, instance.position().z + offset.z);
                if (!instance.entity().level().isEmptyBlock(BlockPos.containing(pos))) {
                    zoom = instance.getMaxZoom(cameraDist * Mod.zoom);
                }
            }
        } else {
            zoom = instance.getMaxZoom(cameraDist);
        }
        return zoom;
    }
}
