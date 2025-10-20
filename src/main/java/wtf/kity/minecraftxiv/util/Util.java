package wtf.kity.minecraftxiv.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ProjectileItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;
import org.joml.Vector3d;
import wtf.kity.minecraftxiv.mixin.GameRendererAccessor;
import wtf.kity.minecraftxiv.mod.Mod;

/**
 * @author ChloeCDN
 */
public class Util {

    public static void debug(String s) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LogManager.getLogger("Minecraft XIV").info(s);
        }
    }

    public static @NotNull Vec3d getMouseRay() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();
        GameRenderer renderer = client.gameRenderer;
        Camera camera = renderer.getCamera();
        float tickProgress = client.getRenderTickCounter().getTickProgress(true);

        Vector2d res = new Vector2d(window.getFramebufferWidth(), window.getFramebufferHeight());
        Vector2d coords = new Vector2d(client.mouse.getX(), client.mouse.getY())
                .div(res)
                .mul(2.0)
                .sub(new Vector2d(1.0));
        double fov2 = Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, tickProgress, true)) / 2.0;
        coords.x *= res.x / res.y;
        coords.y = -coords.y;
        Vector2d offsets = coords.mul(Math.tan(fov2));
        Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
        Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
        Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
        Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
        return new Vec3d(dir.x, dir.y, dir.z);
    }

    public static boolean holdingProjectile(PlayerEntity player) {
        return player.isHolding(item -> item.getItem() instanceof RangedWeaponItem
                || item.getItem() instanceof ProjectileItem);
    }

    /**
     * Determines the point of intersection between a plane defined by a point and a normal vector and a line defined by a point and a direction vector.
     * <p>
     * from https://stackoverflow.com/questions/5666222/3d-line-plane-intersection/52711312#52711312
     *
     * @param planePoint    A point on the plane.
     * @param planeNormal   The normal vector of the plane.
     * @param linePoint     A point on the line.
     * @param lineDirection The normalized direction vector of the line.
     * @return The point of intersection between the line and the plane, null if the line is parallel to the plane.
     */
    public static Vec3d lineIntersection(Vec3d planePoint, Vec3d planeNormal, Vec3d linePoint, Vec3d lineDirection) {
        if (planeNormal.dotProduct(lineDirection) == 0) {
            return null;
        }

        double t = (planeNormal.dotProduct(planePoint) - planeNormal.dotProduct(linePoint)) / planeNormal.dotProduct(lineDirection);
        return linePoint.add(lineDirection.multiply(t));
    }

    private static Vector2d worldToScreen(MinecraftClient client, Vec3d worldCoords) {
        // https://github.com/IamJannik/Pathfinder/blob/1.21/src/client/java/net/bmjo/pathfinder/waypoint/WaypointHUDRenderer.java#L27
        Camera camera = client.gameRenderer.getCamera();
        Vec3d relativePos = worldCoords.subtract(camera.getPos());
        relativePos = relativePos.rotateY((float) Math.toRadians(MathHelper.wrapDegrees(Mod.yaw)));
        relativePos = relativePos.rotateX((float) Math.toRadians(MathHelper.wrapDegrees(Mod.pitch)));
        double scaleFactor = client.getWindow().getScaleFactor();
        float tickProgress = client.getRenderTickCounter().getTickProgress(true);
        double fov = ((GameRendererAccessor) client.gameRenderer).callGetFov(camera, tickProgress, true);
        double scale = client.getWindow().getFramebufferHeight() / scaleFactor / 2.0 / (relativePos.z * Math.tan(Math.toRadians(fov) / 2.0));
        return new Vector2d(-relativePos.x * scale, -relativePos.y * scale);
    }
}
