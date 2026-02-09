package wtf.kity.minecraftxiv.util;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;

//? >=1.21.11 {
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
//? }

/**
 * @author ChloeCDN
 */
public class Util {
    public static void debug(String s) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LogManager.getLogger("Minecraft XIV").info(s);
        }
    }

    public static boolean hotbarHovered() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        MouseHandler mouse = minecraft.mouseHandler;

        double x = mouse.xpos / window.getGuiScale();
        double y = mouse.ypos / window.getGuiScale();
        int screenCenter = window.getGuiScaledWidth() / 2;
        int hotbarWidth = 182;
        int hotbarHeight = 24;
        ScreenRectangle rect = new ScreenRectangle(screenCenter - hotbarWidth / 2,
                window.getGuiScaledHeight() - hotbarHeight,
                hotbarWidth,
                hotbarHeight
        );
        return rectContainsPoint(rect, x, y);
    }

    public static boolean rectContainsPoint(ScreenRectangle rectangle, double x, double y) {
        return x >= rectangle.left() && x < rectangle.right() && y >= rectangle.top() && y < rectangle.bottom();
    }

    public static boolean hasPermissions(Player player) {
        //? <1.21.11 {
        /*return player.hasPermissions(2);
         *///? } else {
        return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
        //? }
    }

    private static @Nullable Double groundedY;
    private static @Nullable Double lastGroundY;
    public static Vec3 getGroundedEyePos() {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        //? <1.21.11 {
        /*Entity entity = camera.getEntity();
        *///? } else {
        Entity entity = camera.entity();
        //? }

        //? <1.21 {
        /*Vec3 pos = entity.getPosition(minecraft.getDeltaFrameTime());
        *///? } else {
        Vec3 pos = entity.getPosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        //? }
        if (groundedY == null || lastGroundY == null) {
            groundedY = pos.y;
            lastGroundY = pos.y;
        }

        BlockHitResult target = entity.level().clip(new ClipContext(
                pos,
                pos.add(new Vec3(0.0, -3.0, 0.0)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                entity
        ));

        if (entity.onGround() || target.getType() == HitResult.Type.MISS || entity.isInWater()) {
            lastGroundY = target.getLocation().y;
        }

        if (groundedY < lastGroundY) {
            groundedY += (lastGroundY - groundedY) * 0.05;
        } else if (!entity.onGround() && groundedY > pos.y) {
            groundedY = pos.y;
            lastGroundY = pos.y;
        }

        return new Vec3(pos.x, groundedY, pos.z);
    }
}
