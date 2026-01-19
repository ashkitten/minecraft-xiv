package wtf.kity.minecraftxiv.mixin.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ServerInit;

@Mixin(Player.class)
public class PlayerEntityMixin {
    @Inject(method = "isWithinBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    public void canInteractWithBlockAt(BlockPos pos, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (ServerInit.capabilities.unlimitedReach()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
