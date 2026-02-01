package wtf.kity.minecraftxiv;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import wtf.kity.minecraftxiv.network.Capabilities;

import java.io.IOException;

public class ServerInit implements DedicatedServerModInitializer {
    public static Capabilities capabilities;

    private static void setCapabilities(Capabilities capabilities) {
        ServerInit.capabilities = capabilities;
        if (capabilities.unlimitedReach()) {
            ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE = Double.POSITIVE_INFINITY;
        } else {
            ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE = Mth.square(6.0);
        }
    }

    @Override
    public void onInitializeServer() {
        setCapabilities(Capabilities.load());

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            packetSender.sendPacket(capabilities);
        });

        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, player, packetSender) -> {
            if (!player.hasPermissions(2)) {
                return;
            }

            if (!payload.equals(capabilities)) {
                setCapabilities(payload);
                try {
                    capabilities.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(other, capabilities);
                }
            }
        });
    }
}
