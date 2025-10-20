package wtf.kity.minecraftxiv.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

public class Config {
    public static final ConfigClassHandler<Config> GSON = ConfigClassHandler
            .createBuilder(Config.class)
            .serializer(config -> GsonConfigSerializerBuilder
                    .create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("minecraftxiv.json"))
                    .build())
            .build();

    @SerialEntry
    public boolean scrollWheelZoom = true;

    public enum RelativeMovement { ALWAYS, NEVER, EXCEPT_MOVE_MODE }
    @SerialEntry
    public RelativeMovement movementCameraRelative = RelativeMovement.EXCEPT_MOVE_MODE;

    @SerialEntry
    public boolean lockOnTargeting = false;

    @SerialEntry
    public boolean targetFromCamera = false;

    @SerialEntry
    public boolean unlimitedReach = false;

    @SerialEntry
    public boolean moveMode = false;

    public enum ProjectileTargeting { NONE, LEGACY, CAMERA_PLANE, VERTICAL_PLANE }
    @SerialEntry
    public ProjectileTargeting projectileTargeting = ProjectileTargeting.LEGACY;
}
