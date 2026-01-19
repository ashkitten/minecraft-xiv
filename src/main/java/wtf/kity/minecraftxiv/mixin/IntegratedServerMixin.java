package wtf.kity.minecraftxiv.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.network.Capabilities;
import wtf.kity.minecraftxiv.util.Util;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "openToLan", at = @At("RETURN"))
    public void openToLan(GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir) {
        Util.debug("Opening to lan (cheats allowed: " + cheatsAllowed + ")");
        if (cheatsAllowed) {
            Capabilities capabilities = Capabilities.load();
            for (ServerPlayerEntity player : ((IntegratedServer)(Object) this).getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, capabilities);
            }
        }
    }
}
