package de.chloedev.cdnperspective.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Invoker("getFov") double callGetFov(Camera camera, float tickDelta, boolean changingFov);
}
