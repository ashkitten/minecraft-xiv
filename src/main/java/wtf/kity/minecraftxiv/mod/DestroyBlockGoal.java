package wtf.kity.minecraftxiv.mod;

import baritone.api.pathing.goals.GoalNear;
import net.minecraft.util.math.BlockPos;

public class DestroyBlockGoal extends GoalNear {
    public DestroyBlockGoal(BlockPos blockPos, int i) {
        super(blockPos, i);
    }
}
