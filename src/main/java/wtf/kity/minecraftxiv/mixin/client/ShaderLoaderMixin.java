package wtf.kity.minecraftxiv.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShaderLoader.class)
public abstract class ShaderLoaderMixin {
    @WrapMethod(method = "getShaderSource")
    private static String getShaderSource(Identifier name, Operation<String> original) {
        try {
            return original.call(Identifier.tryBuild("minecraftxiv", name.getPath()));
        } catch (RuntimeException ignored) {}
        return original.call(name);
    }
}
