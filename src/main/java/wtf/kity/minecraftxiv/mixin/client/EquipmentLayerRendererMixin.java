package wtf.kity.minecraftxiv.mixin.client;

// TODO: make this work on other vers
//? >=1.21.11 {
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
    @Inject(method = "getColorForLayer", at = @At("RETURN"), cancellable = true)
    private static void getColorForLayer(EquipmentClientInfo.Layer layer, int i, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() & Mod.translucencyMask);
        cir.cancel();
    }
}
//? }