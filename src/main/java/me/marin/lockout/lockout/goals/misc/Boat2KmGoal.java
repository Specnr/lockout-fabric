package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class Boat2KmGoal extends Goal implements CustomTextureRenderer {

    private static final ItemStack ITEM_STACK = Items.OAK_BOAT.getDefaultStack();
    public Boat2KmGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Boat 2km";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "2km");
        return true;
    }
}
