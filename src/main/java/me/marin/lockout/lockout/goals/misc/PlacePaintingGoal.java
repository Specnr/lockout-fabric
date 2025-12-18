package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class PlacePaintingGoal extends Goal {

    public PlacePaintingGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Place a Painting";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return Items.PAINTING.getDefaultStack();
    }
}
