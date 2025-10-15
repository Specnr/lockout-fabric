package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FillBundleGoal extends Goal {

    private static final ItemStack ITEM = Items.BUNDLE.getDefaultStack();

    public FillBundleGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Fill Bundle";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }
}
