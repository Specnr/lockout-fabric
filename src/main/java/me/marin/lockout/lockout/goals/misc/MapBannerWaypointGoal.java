package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class MapBannerWaypointGoal extends Goal {

    public MapBannerWaypointGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Mark Banner on Map";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return Items.FILLED_MAP.getDefaultStack();
    }
}
