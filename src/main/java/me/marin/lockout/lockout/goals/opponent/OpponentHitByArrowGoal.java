package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.OpponentGoal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class OpponentHitByArrowGoal extends Goal implements OpponentGoal, CustomTextureRenderer {

    private static final ItemStack ITEM_STACK = Items.ARROW.getDefaultStack();
    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/overlay/no_overlay.png");

    public OpponentHitByArrowGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "All other opponents hit by Arrow";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawItem(ITEM_STACK, x, y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        return true;
    }

}
