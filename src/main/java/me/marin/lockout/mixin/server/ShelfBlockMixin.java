package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillShelfGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShelfBlock;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShelfBlock.class)
public class ShelfBlockMixin {

    @Inject(method = "onUseWithItem", at = @At("RETURN"))
    public void onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient()) return;
        
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) {
            Lockout.log("ShelfBlockMixin: Lockout not running");
            return;
        }

        ShelfBlockEntity blockEntity = (ShelfBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            Lockout.log("ShelfBlockMixin: Block entity is null");
            return;
        }
        
        if (!cir.getReturnValue().isAccepted()) {
            Lockout.log("ShelfBlockMixin: Interaction not successful, result: " + cir.getReturnValue());
            return;
        }

        Lockout.log("ShelfBlockMixin: Interaction successful, checking shelf filled");
        lockout$checkShelfFilled(lockout, player, blockEntity);
    }

    @Unique
    private static void lockout$checkShelfFilled(Lockout lockout, PlayerEntity player, ShelfBlockEntity blockEntity) {
        // Check if all 3 slots of the shelf are filled
        boolean allSlotsFilled = true;
        int filledSlots = 0;
        
        for (int i = 0; i < 3; i++) {
            if (!blockEntity.getStack(i).isEmpty()) {
                filledSlots++;
            } else {
                allSlotsFilled = false;
            }
        }

        Lockout.log("ShelfBlockMixin: Shelf has " + filledSlots + "/3 slots filled, allSlotsFilled: " + allSlotsFilled);

        if (!allSlotsFilled) {
            Lockout.log("ShelfBlockMixin: Shelf not fully filled, returning");
            return;
        }

        Lockout.log("ShelfBlockMixin: Shelf is fully filled, checking goals");
        
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof FillShelfGoal) {
                Lockout.log("ShelfBlockMixin: Found FillShelfGoal, completing for player: " + player.getName().getString());
                lockout.completeGoal(goal, player);
                return;
            }
        }
        
        Lockout.log("ShelfBlockMixin: No FillShelfGoal found on board");
    }
}

