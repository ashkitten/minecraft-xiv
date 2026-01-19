package wtf.kity.minecraftxiv;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import wtf.kity.minecraftxiv.network.Capabilities;

import java.io.IOException;

public class ServerInit implements DedicatedServerModInitializer {
    public static Capabilities capabilities;

    @Override
    public void onInitializeServer() {
        capabilities = Capabilities.load();

        PayloadTypeRegistry.clientboundPlay().register(Capabilities.ID, Capabilities.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Capabilities.ID, Capabilities.CODEC);

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            packetSender.sendPacket(capabilities);
        });

        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            if (!context.player().permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
                return;
            }

            if (!payload.equals(capabilities)) {
                capabilities = payload;
                try {
                    capabilities.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, capabilities);
                }
            }
        });
    }
}
