package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void renderSlot(GuiGraphics graphics, int x, int y, float tickDelta, Player player, ItemStack itemStack, int seed);

    @Shadow
    public abstract void tick(boolean bl);

    @Redirect(
            method = "renderCrosshair",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean isFirstPerson(CameraType perspective) {
        if (perspective.isFirstPerson()) {
            return true;
        }
        if (!Mod.enabled) {
            return false;
        }
        return !ClientInit.moveCameraBinding.isDown();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void crosshairPre(GuiGraphics context, CallbackInfo ci) {
        if (Mod.enabled) {
            double scaleFactor = minecraft.getWindow().getGuiScale();
            MouseHandler mouse = minecraft.mouseHandler;

            context.pose().pushPose();
            context.pose().translate(
                    (float) (-context.guiWidth() / 2d + mouse.xpos() / scaleFactor),
                    (float) (-context.guiHeight() / 2f + mouse.ypos() / scaleFactor),
                    0.0f
            );
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void crosshairPost(GuiGraphics context, CallbackInfo ci) {
        if (Mod.enabled) {
            context.pose().popPose();
        }
    }

    @Redirect(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IIFLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
            ordinal = 0
        )
    )
    private void renderSlot(Gui instance, GuiGraphics graphics, int x, int y, float tickDelta, Player player, ItemStack itemStack, int seed, @Local(name = "m") int m) {
        if (Mod.enabled) {
            MouseHandler mouse = minecraft.mouseHandler;

            ScreenRectangle rect = new ScreenRectangle(x - 2, y - 2, 20, 20);
            double xpos = mouse.xpos / this.minecraft.getWindow().getGuiScale();
            double ypos = mouse.ypos / this.minecraft.getWindow().getGuiScale();
            if (Util.rectContainsPoint(rect, xpos, ypos)) {
                graphics.renderOutline(x - 2, y - 2, 20, 20, 0xFFFFFFFF);

                if (mouse.isLeftPressed()) {
                    player.getInventory().selected = m;
                }
            }
        }

        renderSlot(graphics, x, y, tickDelta, player, itemStack, seed);
    }
}
