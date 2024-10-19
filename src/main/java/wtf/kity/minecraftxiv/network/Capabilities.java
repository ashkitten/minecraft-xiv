package wtf.kity.minecraftxiv.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public record Capabilities(boolean targetFromCamera) implements CustomPayload {
    public static final CustomPayload.Id<Capabilities> ID = new CustomPayload.Id<>(Identifier.of("minecraftxiv", "capabilities"));
    public static final PacketCodec<RegistryByteBuf, Capabilities> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, Capabilities::targetFromCamera,
            Capabilities::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static Capabilities all() {
        return new Capabilities(true);
    }

    public static Capabilities none() {
        return new Capabilities(false);
    }

    public static Capabilities load() {
        try {
            FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve("minecraftxiv.capabilities.json").toFile());
            return new Gson().fromJson(reader, Capabilities.class);
        } catch (FileNotFoundException e) {
            return none();
        }
    }

    public static void save(Capabilities capabilities) throws IOException {
        FileWriter writer = new FileWriter(FabricLoader.getInstance().getConfigDir().resolve("minecraftxiv.capabilities.json").toFile());
        new GsonBuilder().setPrettyPrinting().create().toJson(capabilities, Capabilities.class, writer);
        writer.close();
    }
}
