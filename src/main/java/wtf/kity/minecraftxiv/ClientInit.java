package wtf.kity.minecraftxiv;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
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
    public static Capabilities capabilities;

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
        return serverSupportsCapabilities() && client.player != null && client.player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
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

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("minecraftxiv.binds.category"));

        KeyMappingHelper.registerKeyMapping(toggleBinding = new KeyMapping(
                "minecraftxiv.binds.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                 category
        ));
        KeyMappingHelper.registerKeyMapping(moveCameraBinding = new KeyMapping(
                "minecraftxiv.binds.moveCamera",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_3,
                category
        ));
        KeyMappingHelper.registerKeyMapping(zoomInBinding = new KeyMapping(
                "minecraftxiv.binds.zoomIn",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_6,
                category
        ));
        KeyMappingHelper.registerKeyMapping(zoomOutBinding = new KeyMapping(
                "minecraftxiv.binds.zoomOut",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_7,
                category
        ));
        KeyMappingHelper.registerKeyMapping(cycleTargetBinding = new KeyMapping(
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

        PayloadTypeRegistry.clientboundPlay().register(Capabilities.ID, Capabilities.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Capabilities.ID, Capabilities.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            capabilities = payload;
            notifyCapabilityListeners();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, minecraftClient) -> capabilities = null);

        // Server side stuff

        ServerLifecycleEvents.SERVER_STARTED.register((minecraftServer) -> capabilities = Capabilities.none());

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            if (networkHandler.player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
                capabilities = Capabilities.load();
                packetSender.sendPacket(capabilities);
            }
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