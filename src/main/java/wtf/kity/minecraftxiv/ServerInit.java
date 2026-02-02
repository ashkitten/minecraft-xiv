package wtf.kity.minecraftxiv;

import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import wtf.kity.minecraftxiv.network.Capabilities;
import wtf.kity.minecraftxiv.util.Util;

import java.io.IOException;

@Entrypoint
public class ServerInit implements DedicatedServerModInitializer {
    public static Capabilities capabilities;

    private static void setCapabilities(Capabilities capabilities) {
        ServerInit.capabilities = capabilities;
        //? <1.21 {
        /*if (capabilities != null && capabilities.unlimitedReach()) {
            ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE = Double.POSITIVE_INFINITY;
        } else {
            ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE = Mth.square(6.0);
        }
        *///? }
    }

    @Override
    public void onInitializeServer() {
        setCapabilities(Capabilities.load());

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            packetSender.sendPacket(capabilities);
        });

        //? <1.20.5 {
        /*ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, player, responseSender) -> {
            MinecraftServer server = player.getServer();
         *///? } else {
        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
        //? }
            if (!Util.hasPermissions(player)) {
                return;
            }

            if (!payload.equals(capabilities)) {
                setCapabilities(payload);
                try {
                    capabilities.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(other, capabilities);
                }
            }
        });
    }
}
