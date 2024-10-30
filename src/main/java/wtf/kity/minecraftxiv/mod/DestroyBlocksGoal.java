package wtf.kity.minecraftxiv.mod;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;
import java.util.List;

public class DestroyBlocksGoal implements Goal {
    public List<BlockPos> blocks;

    public DestroyBlocksGoal(List<BlockPos> blocks) {
        this.blocks = blocks;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        assert client.world != null && player != null;

        Vec3d eyePos = new BlockPos(x, y, z).toBottomCenterPos().add(0, player.getStandingEyeHeight(), 0);

        return blocks
                .stream()
                .anyMatch(pos -> {
                    BlockHitResult hitResult =
                            client.world.raycast(new RaycastContext(
                                    eyePos,
                                    pos.toCenterPos(),
                                    RaycastContext.ShapeType.OUTLINE,
                                    RaycastContext.FluidHandling.NONE,
                                    player
                            ));
                    return eyePos.distanceTo(hitResult.getPos()) < player.getBlockInteractionRange()
                            && hitResult.getBlockPos().equals(pos);
                });
    }

    @Override
    public double heuristic(int x, int y, int z) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        assert client.world != null && player != null;

        BlockPos blockPos = new BlockPos(x, y, z);
        Vec3d eyePos = blockPos.toBottomCenterPos().add(0, player.getStandingEyeHeight(), 0);

        return blocks
                .stream()
                .map(pos -> {
                    BlockHitResult hitResult =
                            client.world.raycast(new RaycastContext(
                                    eyePos,
                                    pos.toCenterPos(),
                                    RaycastContext.ShapeType.OUTLINE,
                                    RaycastContext.FluidHandling.NONE,
                                    player
                            ));
                    BlockPos diff = pos.subtract(blockPos);
                    return GoalBlock.calculate(diff.getX(), diff.getY(), diff.getZ())
                            + (hitResult.getBlockPos().equals(pos) ? eyePos.distanceTo(hitResult.getPos()) : 5.0);
                })
                .min(Comparator.naturalOrder())
                .orElse(0.0);
    }
}
