package wtf.kity.minecraftxiv.network;

import com.google.gson.*;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public record Capabilities(boolean targetFromCamera, boolean unlimitedReach) implements FabricPacket {
    public static final PacketType<Capabilities> ID = PacketType.create(new ResourceLocation("minecraftxiv", "capabilities"), Capabilities::new);

    public Capabilities(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(targetFromCamera);
        buf.writeBoolean(unlimitedReach);
    }

    public static Capabilities all() {
        return new Capabilities(true, true);
    }

    public static Capabilities none() {
        return new Capabilities(false, false);
    }

    public static Capabilities load() {
        try {
            FileReader reader = new FileReader(FabricLoader
                    .getInstance()
                    .getConfigDir()
                    .resolve("minecraftxiv.capabilities.json")
                    .toFile());
            return new GsonBuilder()
                    .registerTypeAdapter(Capabilities.class, new CapabilitiesDeserializer())
                    .create()
                    .fromJson(reader, Capabilities.class);
        } catch (FileNotFoundException e) {
            return Capabilities.none();
        }
    }

    @Override
    public PacketType<? extends FabricPacket> getType() {
        return ID;
    }

    public Capabilities withTargetFromCamera(boolean targetFromCamera) {
        return new Capabilities(targetFromCamera, this.unlimitedReach);
    }

    public Capabilities withUnlimitedReach(boolean unlimitedReach) {
        return new Capabilities(this.targetFromCamera, unlimitedReach);
    }

    public void save() throws IOException {
        FileWriter writer = new FileWriter(FabricLoader
                .getInstance()
                .getConfigDir()
                .resolve("minecraftxiv.capabilities.json")
                .toFile());
        new GsonBuilder().setPrettyPrinting().create().toJson(this, Capabilities.class, writer);
        writer.close();
    }

    private static class CapabilitiesDeserializer implements JsonDeserializer<Capabilities> {
        @Override
        public Capabilities deserialize(
                JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context
        ) throws JsonParseException {
            Capabilities capabilities = Capabilities.none();
            if (json instanceof JsonObject object) {
                if (object.get("targetFromCamera") instanceof JsonPrimitive primitive) {
                    capabilities = capabilities.withTargetFromCamera(primitive.getAsBoolean());
                }
                if (object.get("unlimitedReach") instanceof JsonPrimitive primitive) {
                    capabilities = capabilities.withUnlimitedReach(primitive.getAsBoolean());
                }
            }
            return capabilities;
        }
    }
}
