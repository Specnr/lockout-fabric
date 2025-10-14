package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class OpponentHitBySnowballGoal extends Goal {

    public OpponentHitBySnowballGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "All other opponents hit by Snowball";
    }

    private static final ItemStack ITEM_STACK = Items.SNOWBALL.getDefaultStack();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}