package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Redirect(
        method = "pick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"
        )
    )
    private HitResult pick(Entity instance, double d, float f, boolean bl) {
        if (Config.GSON.instance().lockOnTargeting && Mod.lockOnTarget != null) {
            instance.lookAt(EntityAnchorArgument.Anchor.EYES, Mod.lockOnTarget.getEyePosition());
            return new EntityHitResult(Mod.lockOnTarget);
        } else if (Config.GSON.instance().targetFromCamera && ClientInit.getCapabilities().targetFromCamera() && Mod.crosshairTarget != null) {
            if (Config.GSON.instance().unlimitedReach && ClientInit.getCapabilities().unlimitedReach() || Mod.crosshairTarget.distanceTo(instance) < Mth.square(d)) {
                return Mod.crosshairTarget;
            } else {
                return new HitResult(Mod.crosshairTarget.getLocation()) {
                    @Override
                    public @NotNull Type getType() {
                        return Type.MISS;
                    }
                };
            }
        }
        return instance.pick(d, f, bl);
    }
}
