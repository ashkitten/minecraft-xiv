package wtf.kity.minecraftxiv.mixin;

import com.mojang.datafixers.DataFixer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.network.Capabilities;
import wtf.kity.minecraftxiv.util.Util;

import java.net.Proxy;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {
    public IntegratedServerMixin(Thread serverThread, LevelStorageSource.LevelStorageAccess storageSource, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer fixerUpper, Services services, LevelLoadListener levelLoadListener) {
        super(serverThread, storageSource, packRepository, worldStem, proxy, fixerUpper, services, levelLoadListener);
    }

    @Inject(method = "publishServer", at = @At("RETURN"))
    public void openToLan(GameType gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir) {
        Util.debug("Opening to lan (cheats allowed: " + cheatsAllowed + ")");
        if (cheatsAllowed) {
            Capabilities capabilities = Capabilities.load();
            for (ServerPlayer player : getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, capabilities);
            }
        }
    }
}
