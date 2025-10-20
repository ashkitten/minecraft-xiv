package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "getEntitiesToRender", at = @At("TAIL"))
    void getEntitiesToRender(Camera camera, Frustum frustum, List<Entity> output, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = MinecraftClient.getInstance().player;

        if (player != null && player.isControlledByPlayer() && Mod.enabled && Config.GSON.instance().lockOnTargeting
                && ClientInit.cycleTargetBinding.wasPressed()) {
            Mod.updateLockOnTarget(camera, output, player);
        }
    }
}
