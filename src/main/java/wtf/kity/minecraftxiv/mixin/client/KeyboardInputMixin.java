package wtf.kity.minecraftxiv.mixin.client;

import net.minecraft.client.Minecraft;
//? <1.21.11 {
/*import net.minecraft.client.player.Input;
*///? } else {
import net.minecraft.client.player.ClientInput;
//? }
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
//? <1.21.11 {
/*public abstract class KeyboardInputMixin extends Input {
*///? } else {
public abstract class KeyboardInputMixin extends ClientInput {
//? }
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {
        if (Mod.enabled && Config.GSON.instance().movementCameraRelative) {
            Minecraft client = Minecraft.getInstance();
            assert client.player != null;
            Vector2f movement = new Vector2f(this.getMoveVector().y, this.getMoveVector().x);
            //? <1.21.11 {
            /*float yaw = client.gameRenderer.getMainCamera().getYRot() - client.player.getVisualRotationYInDegrees();
            *///? } else {
            float yaw = client.gameRenderer.getMainCamera().yRot() - client.player.getVisualRotationYInDegrees();
            //? }
            movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            //? <1.21.11 {
            /*this.leftImpulse = movement.y;
            this.forwardImpulse = movement.x;
            *///? } else {
            this.moveVector = new Vec2(movement.y, movement.x);
            //? }
        }
    }
}
