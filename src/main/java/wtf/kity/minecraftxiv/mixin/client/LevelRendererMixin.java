package wtf.kity.minecraftxiv.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.util.ARGB;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @ModifyArg(
            method = "addMainPass",
            at = @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"
            )
    )
    private Runnable executes(Runnable runnable) {
        return () -> {
            if (Mod.enabled) {
                RenderTarget renderTarget = this.minecraft.getMainRenderTarget();
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 0.0);
                Mod.doCulling = false;
                GL11.glDepthFunc(GL11.GL_GREATER);
                runnable.run();
                Mod.doCulling = true;
                GL11.glDepthFunc(GL11.GL_LESS);
            } else {
                Mod.doCulling = false;
            }
            runnable.run();
            // Reset
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        };
    }
}
