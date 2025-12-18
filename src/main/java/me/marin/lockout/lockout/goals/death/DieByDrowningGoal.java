package me.marin.lockout.lockout.goals.death;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class DieByDrowningGoal extends Goal implements CustomTextureRenderer {

    private static final Identifier BUBBLE_TEXTURE = Identifier.ofVanilla("mob_effect/water_breathing");
    private static final Identifier OVERLAY_TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/overlay/die_to_overlay.png");

    public DieByDrowningGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Die by drowning";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BUBBLE_TEXTURE, x, y, 16, 16);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, OVERLAY_TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        return true;
    }
}
