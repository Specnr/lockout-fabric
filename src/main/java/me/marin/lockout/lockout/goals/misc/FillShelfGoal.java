package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FillShelfGoal extends Goal {

    private static final ItemStack ITEM_STACK = Items.OAK_SHELF.getDefaultStack();

    public FillShelfGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Fill a Shelf";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }
}
