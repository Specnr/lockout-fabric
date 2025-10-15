package me.marin.lockout.mixin.server;

import me.marin.lockout.CompassItemHandler;
import me.marin.lockout.Lockout;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.item.CompassItem.class)
public class CompassItemUseOnBlockMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void onUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        BlockPos blockPos = context.getBlockPos();
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack itemStack = context.getStack();

        // Only check if it's a lodestone and we're in a lockout game
        if (!world.getBlockState(blockPos).isOf(Blocks.LODESTONE)) return;
        if (world.isClient()) return;
        if (player == null) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!lockout.isLockoutPlayer(player.getUuid())) return;

        // Check if this is a tracking compass
        if (CompassItemHandler.isCompass(itemStack)) {
            // Block tracking compasses from being used on lodestones entirely
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // Regular compasses proceed normally - they can be used on lodestones
        // and will complete the advancement goal through the normal advancement system
    }
}
