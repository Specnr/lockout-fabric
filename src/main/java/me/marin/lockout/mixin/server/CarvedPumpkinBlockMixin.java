package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.ConstructCopperGolemGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {

    @Inject(method = "trySpawnEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/CopperGolemEntity;onSpawn(Lnet/minecraft/block/Oxidizable$OxidationLevel;)V"))
    public void onCopperGolemSpawn(World world, BlockPos pos, CallbackInfo ci) {
        if (world.isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        lockout$onCopperGolemSpawn(lockout, (ServerWorld) world, pos);
    }

    @Unique
    private static void lockout$onCopperGolemSpawn(Lockout lockout, ServerWorld world, BlockPos pos) {
        // Find the nearest player to the copper golem spawn location
        var players = world.getPlayers();
        if (players.isEmpty()) return;

        var nearestPlayer = players.stream()
                .min((p1, p2) -> {
                    double dist1 = p1.getBlockPos().getSquaredDistance(pos);
                    double dist2 = p2.getBlockPos().getSquaredDistance(pos);
                    return Double.compare(dist1, dist2);
                })
                .orElse(null);

        if (nearestPlayer == null) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof ConstructCopperGolemGoal) {
                lockout.completeGoal(goal, nearestPlayer);
            }
        }
    }
}
