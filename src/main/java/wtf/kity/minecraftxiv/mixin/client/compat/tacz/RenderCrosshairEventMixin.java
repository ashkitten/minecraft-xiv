package wtf.kity.minecraftxiv.mixin.client.compat.tacz;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.mod.Mod;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.RenderCrosshairEvent")
public class RenderCrosshairEventMixin {
    @Inject(method = "onRenderOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderOverlayPre(@Coerce Object event, CallbackInfo ci) {
        if (Mod.enabled) {
            ci.cancel();
        }
    }
}
