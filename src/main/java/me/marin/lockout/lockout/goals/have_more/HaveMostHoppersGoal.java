package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class HaveMostHoppersGoal extends Goal implements CustomTextureRenderer {

    private static final ItemStack ITEM_STACK = Items.HOPPER.getDefaultStack();
    
    public HaveMostHoppersGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have the most Hoppers";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/up_arrow.png");
    
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawItem(ITEM_STACK, x, y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }
}
