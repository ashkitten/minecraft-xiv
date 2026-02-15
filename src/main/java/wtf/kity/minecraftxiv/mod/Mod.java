package wtf.kity.minecraftxiv.mod;

import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Unique;

public class Mod {
    public static float yaw;
    public static float pitch;
    public static float zoom = 1.0f;
    public static boolean enabled = false;
    public static boolean moving = false;
    public static CameraType lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity lockOnTarget;
    public static int translucencyMask = 0xFFFFFFFF;
    /// whether this is the second pass where we should be culling
    public static boolean doCulling;
}