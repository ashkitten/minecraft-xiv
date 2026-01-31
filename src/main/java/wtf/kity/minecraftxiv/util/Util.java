package wtf.kity.minecraftxiv.util;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.apache.logging.log4j.LogManager;

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

        int x = (int) mouse.getScaledXPos(window);
        int y = (int) mouse.getScaledYPos(window);
        int screenCenter = window.getGuiScaledWidth() / 2;
        int hotbarWidth = 182;
        int hotbarHeight = 24;
        ScreenRectangle rect = new ScreenRectangle(screenCenter - hotbarWidth / 2,
                window.getGuiScaledHeight() - hotbarHeight,
                hotbarWidth,
                hotbarHeight
        );
        return rect.containsPoint(x, y);
    }
}
