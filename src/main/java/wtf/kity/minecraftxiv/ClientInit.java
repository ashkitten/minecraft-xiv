package wtf.kity.minecraftxiv;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.network.Capabilities;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    public static ClientInit instance;
    private static final ArrayList<Consumer<Capabilities>> capabilityListeners = new ArrayList<>();
    public static KeyMapping toggleBinding;
    public static KeyMapping moveCameraBinding;
    public static KeyMapping zoomInBinding;
    public static KeyMapping zoomOutBinding;
    public static KeyMapping cycleTargetBinding;

    @Nullable
    private static Capabilities capabilities;

    public static boolean serverSupportsCapabilities() {
        return capabilities != null;
    }

    public static boolean isDisconnected() {
        ClientPacketListener clientPlayNetworkHandler = Minecraft.getInstance().getConnection();
        return clientPlayNetworkHandler == null || !clientPlayNetworkHandler.getConnection().isConnected();
    }

    public static boolean canChangeCapabilities() {
        Minecraft client = Minecraft.getInstance();
        // We need to have received capabilities from the server already, and have adequate permissions to change them
        return serverSupportsCapabilities() && client.player != null && client.player.hasPermissions(2);
    }

    public static Capabilities getCapabilities() {
        if (isDisconnected()) {
            return Capabilities.all();
        }

        if (!serverSupportsCapabilities()) {
            return Capabilities.none();
        }

        return capabilities;
    }

    public static void setCapabilities(@Nullable Capabilities capabilities) {
        ClientInit.capabilities = capabilities;
        if (capabilities != null && capabilities.unlimitedReach()) {
            ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE = Double.POSITIVE_INFINITY;
        } else {
            ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE = Mth.square(6.0);
        }
    }

    public static void submitCapabilities(Capabilities capabilities) {
        if (isDisconnected()) return;
        ClientPlayNetworking.send(capabilities);
    }

    public static void listenCapabilities(Consumer<Capabilities> listener) {
        capabilityListeners.add(listener);
    }

    public static void unlistenCapabilities(Consumer<Capabilities> listener) {
        capabilityListeners.removeIf(l -> l == listener);
    }

    public static void notifyCapabilityListeners() {
        // Iterate backwards in case it decides to remove itself during execution
        // Rust wouldn't have let me make this mistake >:c
        for (int i = capabilityListeners.size() - 1; i >= 0; i--) {
            capabilityListeners.get(i).accept(getCapabilities());
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        Config.GSON.load();

        String category = "minecraftxiv.binds.category";

        KeyBindingHelper.registerKeyBinding(toggleBinding = new KeyMapping(
                "minecraftxiv.binds.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                category
        ));
        KeyBindingHelper.registerKeyBinding(moveCameraBinding = new KeyMapping(
                "minecraftxiv.binds.moveCamera",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_3,
                category
        ));
        KeyBindingHelper.registerKeyBinding(zoomInBinding = new KeyMapping(
                "minecraftxiv.binds.zoomIn",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_6,
                category
        ));
        KeyBindingHelper.registerKeyBinding(zoomOutBinding = new KeyMapping(
                "minecraftxiv.binds.zoomOut",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_7,
                category
        ));
        KeyBindingHelper.registerKeyBinding(cycleTargetBinding = new KeyMapping(
                "minecraftxiv.binds.cycleTarget",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,
                category
        ));

        listenCapabilities(capabilities -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.literal("§7[§5MinecraftXIV§7] §rAllowed features:"), false);
                player.displayClientMessage(Component
                        .translatable("minecraftxiv.config.targetFromCamera.name")
                        .withStyle(capabilities.targetFromCamera() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
                player.displayClientMessage(Component
                        .translatable("minecraftxiv.config.unlimitedReach.name")
                        .withStyle(capabilities.unlimitedReach() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
            }
        });

        // Client side stuff

        ClientPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, player, responseSender) -> {
            setCapabilities(payload);
            notifyCapabilityListeners();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, minecraftClient) -> setCapabilities(null));

        // Server side stuff

        ServerLifecycleEvents.SERVER_STARTED.register((minecraftServer) -> setCapabilities(Capabilities.none()));

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            if (networkHandler.player.hasPermissions(2)) {
                packetSender.sendPacket(capabilities);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, player, responseSender) -> {
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