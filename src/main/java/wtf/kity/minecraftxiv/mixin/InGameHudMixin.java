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

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    abstract void renderSlot(GuiGraphics graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed);

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

    @Redirect(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    private void renderSlot(Gui instance, GuiGraphics graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed, @Local(name = "i") int i) {
        if (Mod.enabled) {
            MouseHandler mouse = minecraft.mouseHandler;

            ScreenRectangle rect = new ScreenRectangle(x, y, 16, 16);
            if (rect.containsPoint((int) mouse.getScaledXPos(this.minecraft.getWindow()), (int) mouse.getScaledYPos(this.minecraft.getWindow()))) {
                graphics.renderOutline(x, y, 16, 16, 0xFFFFFFFF);

                if (mouse.isLeftPressed()) {
                    player.getInventory().setSelectedSlot(i);
                }
            }
        }

        renderSlot(graphics, x, y, deltaTracker, player, itemStack, seed);
    }
}
