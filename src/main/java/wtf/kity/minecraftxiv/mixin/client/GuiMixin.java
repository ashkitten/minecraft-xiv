package wtf.kity.minecraftxiv.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

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
    private void crosshairPre(GuiGraphics context, /*?>=1.21>>+','*/ DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Mod.enabled) {
            double scaleFactor = minecraft.getWindow().getGuiScale();
            MouseHandler mouse = minecraft.mouseHandler;

            context.pose().pushMatrix();
            context.pose().translate(
                    (float) (-context.guiWidth() / 2d + mouse.xpos() / scaleFactor),
                    (float) (-context.guiHeight() / 2f + mouse.ypos() / scaleFactor)
                    //? <1.21.11
                    //, 0.0f
            );
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void crosshairPost(GuiGraphics context, /*?>=1.21>>+','*/ DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Mod.enabled) {
            context.pose().popMatrix();
        }
    }

    //? <1.21 {
    /*@WrapOperation(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IIFLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
                    ordinal = 0
            )
    )
    private void renderSlot(
            Gui instance,
            GuiGraphics graphics, int x, int y, float delta, Player player, ItemStack itemStack, int seed,
            Operation<Void> original,
            @Local(ordinal = 4) int i
    ) {
    *///? } else {
    @WrapOperation(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
                    ordinal = 0
            )
    )
    private void renderSlot(
        Gui instance,
        GuiGraphics graphics, int x, int y, DeltaTracker delta, Player player, ItemStack itemStack, int seed,
        Operation<Void> original,
        @Local(ordinal = 4) int i
    ) {
    //? }
        original.call(instance, graphics, x, y, delta, player, itemStack, seed);
        if (Mod.enabled) {
            MouseHandler mouse = minecraft.mouseHandler;

            ScreenRectangle rect = new ScreenRectangle(x - 2, y - 2, 20, 20);
            double xpos = mouse.xpos / this.minecraft.getWindow().getGuiScale();
            double ypos = mouse.ypos / this.minecraft.getWindow().getGuiScale();
            if (Util.rectContainsPoint(rect, xpos, ypos)) {
                graphics.renderOutline(x - 2, y - 2, 20, 20, 0xFFFFFFFF);

                if (mouse.isLeftPressed()) {
                    //? <1.21.11 {
                    /*player.getInventory().selected = i;
                    *///? } else {
                    player.getInventory().setSelectedSlot(i);
                    //? }
                }
            }
        }
    }
}
