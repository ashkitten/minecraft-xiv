package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

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
    private void crosshairPre(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            double scaleFactor = minecraft.getWindow().getGuiScale();
            MouseHandler mouse = minecraft.mouseHandler;

            //Using RenderSystem on purpose.
            //The f3 "axes" debug cursor calls RenderSystem directly instead of using matrix stack.
            context.pose().pushMatrix();
            context.pose().translate(
                    (float) (-context.guiWidth() / 2d + mouse.xpos() / scaleFactor),
                    (float) (-context.guiHeight() / 2f + mouse.ypos() / scaleFactor)
            );
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void crosshairPost(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            context.pose().popMatrix();
        }
    }
}
