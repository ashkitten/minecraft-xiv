package wtf.kity.minecraftxiv.mod;

import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public class Mod {
    public static float yaw;
    public static float pitch;
    public static float zoom = 1.0f;
    public static boolean enabled = false;
    public static CameraType lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity lockOnTarget;
}