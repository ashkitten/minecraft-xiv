package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Shadow
    private Vec3d pos;

    private Vec3d center;

    @Shadow
    @Final
    private BlockPos.Mutable blockPos;

    @Shadow
    private Entity focusedEntity;

    @Shadow
    private float cameraY;

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;setPos(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 0
            )
    )
    public void setPos(Camera instance, Vec3d pos) {
        if (Mod.enabled && Mod.zoom > 0.0) {
            Vec3d diff = pos.subtract(this.center);
            double dist = Math.max(0.0, (diff.length() - Mod.zoom));
            this.center =
                    this.center.add(diff.normalize().multiply(Math.min(
                            dist * 0.01,
                            Math.sqrt(diff.length())
                    )));
        } else {
            this.center = pos;
        }
        this.pos = this.center;
        this.blockPos.set(this.pos.x, this.pos.y, this.pos.z);
    }

    @Redirect(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", ordinal = 0)
    )
    public void setPos(Camera instance, double x, double y, double z) {
        setPos(instance, new Vec3d(x, y, z));
    }

    @Redirect(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F", ordinal = 0)
    )
    public float clipToSpace(Camera instance, float f) {
        if (Mod.enabled) {
            this.setRotation(Mod.yaw, Mod.pitch);
            //return this.clipToSpace(f * Mod.zoom);
            return f * Mod.zoom;
        } else {
            return this.clipToSpace(f);
        }
    }
}
