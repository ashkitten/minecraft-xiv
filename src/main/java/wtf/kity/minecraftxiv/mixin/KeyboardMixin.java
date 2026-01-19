package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Redirect(
            method = "keyPress",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V")
    )
    void openGameMenu(Minecraft instance, boolean pauseOnly) {
        if (Mod.lockOnTarget != null) {
            Mod.lockOnTarget = null;
        } else {
            instance.pauseGame(pauseOnly);
        }
    }
}
