package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {
        if (Mod.enabled && Config.GSON.instance().movementCameraRelative) {
            Minecraft client = Minecraft.getInstance();
            assert client.player != null;
            //noinspection SuspiciousNameCombination
            Vector2f movement = new Vector2f(this.getMoveVector().y, this.getMoveVector().x);
            float yaw = client.gameRenderer.getMainCamera().getYRot() - client.player.getVisualRotationYInDegrees();
            movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            //noinspection SuspiciousNameCombination
            this.leftImpulse = movement.y;
            this.forwardImpulse = movement.x;
        }
    }
}
