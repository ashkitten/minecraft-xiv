package wtf.kity.minecraftxiv.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Redirect(method = "updateSource", at = @At(
            value = "INVOKE",
            //? <1.21.11 {
            /*target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"
            *///? } else {
            target = "Lnet/minecraft/client/Camera;position()Lnet/minecraft/world/phys/Vec3;"
            //? }
            ))
    public Vec3 position(Camera instance) {
        if (Mod.enabled) {
            //? <1.21.11 {
            /*return instance.getEntity().position();
            *///? } else {
            return instance.entity().position();
            //? }
        } else {
            //? <1.21.11 {
            /*return instance.getPosition();
            *///? } else {
            return instance.position();
            //? }
        }
    }
}
