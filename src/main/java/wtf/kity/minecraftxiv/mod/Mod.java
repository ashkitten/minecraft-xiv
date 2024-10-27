package wtf.kity.minecraftxiv.mod;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.collection.ArrayListDeque;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Objects;

public class Mod {
    public static float yaw;
    public static float pitch;
    public static float zoom = 1.0f;
    public static boolean enabled = false;
    public static Perspective lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity lockOnTarget;
    public static ArrayListDeque<Goal> goals = new ArrayListDeque<>();

    // adapted from https://codeberg.org/MicrocontrollersDev/Simple-Block-Overlay/src/branch/1.20/src/main/java/dev/microcontrollers/simpleblockoverlay/util/RenderUtil.java
    public static void renderOverlays(WorldRenderContext context) {
        if (Mod.goals.isEmpty()) return;

        ClientWorld world = context.world();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float cameraX = (float) context.camera().getPos().getX();
        float cameraY = (float) context.camera().getPos().getY();
        float cameraZ = (float) context.camera().getPos().getZ();

        for (Goal goal : Mod.goals) {
            if (goal instanceof DestroyBlockGoal destroyBlock) {
                BlockPos pos = destroyBlock.getGoalPos();
                BlockState state = world.getBlockState(pos);
                drawBox(context, buffer, pos, cameraX, cameraY, cameraZ, Color.RED, state, world);
            } else if (goal instanceof GoalNear near) {
                BlockPos pos = near.getGoalPos();
                BlockState state = world.getBlockState(pos);
                drawBox(context, buffer, pos, cameraX, cameraY, cameraZ, Color.GREEN, state, world);
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    private static void drawBox(
            WorldRenderContext context,
            BufferBuilder buffer,
            BlockPos pos,
            float cameraX,
            float cameraY,
            float cameraZ,
            Color color,
            BlockState state,
            ClientWorld world
    ) {
        VoxelShapes.BoxConsumer boxConsumer = (minX, minY, minZ, maxX, maxY, maxZ) -> {
            // We make the cube a tiny bit larger to avoid z fighting
            double zFightingOffset = 0.001;

            VertexRendering.drawFilledBox(
                    Objects.requireNonNull(context.matrixStack()),
                    buffer,
                    (float) (pos.getX() + minX - cameraX - zFightingOffset),
                    (float) (pos.getY() + minY - cameraY - zFightingOffset),
                    (float) (pos.getZ() + minZ - cameraZ - zFightingOffset),
                    (float) (pos.getX() + maxX - cameraX + zFightingOffset),
                    (float) (pos.getY() + maxY - cameraY + zFightingOffset),
                    (float) (pos.getZ() + maxZ - cameraZ + zFightingOffset),
                    color.getRed() / 255.0f,
                    color.getGreen() / 255.0f,
                    color.getBlue() / 255.0f,
                    0.5f
            );
        };

        if (state.getOutlineShape(world, pos).isEmpty()) {
            boxConsumer.consume(0, 0, 0, 1, 0.1, 1);
        } else {
            state.getOutlineShape(world, pos).forEachBox(boxConsumer);
        }
    }
}